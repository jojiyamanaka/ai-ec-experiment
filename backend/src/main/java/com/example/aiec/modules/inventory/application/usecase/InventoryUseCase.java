package com.example.aiec.modules.inventory.application.usecase;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.inventory.adapter.dto.AvailabilityDto;
import com.example.aiec.modules.inventory.adapter.dto.InventoryStatusDto;
import com.example.aiec.modules.inventory.adapter.dto.ReservationDto;
import com.example.aiec.modules.inventory.adapter.dto.StockShortageDetail;
import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
import com.example.aiec.modules.inventory.application.port.InventoryQueryPort;
import com.example.aiec.modules.inventory.domain.entity.InventoryAdjustment;
import com.example.aiec.modules.inventory.domain.entity.StockReservation;
import com.example.aiec.modules.inventory.domain.entity.StockReservation.ReservationType;
import com.example.aiec.modules.inventory.domain.repository.InventoryAdjustmentRepository;
import com.example.aiec.modules.inventory.domain.repository.StockReservationRepository;
import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.product.domain.repository.ProductRepository;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ConflictException;
import com.example.aiec.modules.shared.exception.InsufficientStockException;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 在庫ユースケース（Port実装）
 */
@Service
@RequiredArgsConstructor
@Slf4j
class InventoryUseCase implements InventoryQueryPort, InventoryCommandPort {

    private static final int RESERVATION_EXPIRY_MINUTES = 30;

    private final StockReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public ReservationDto createReservation(String sessionId, Long productId, Integer quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        Instant now = Instant.now();

        var existingReservation = reservationRepository.findActiveTentative(sessionId, productId, now);

        if (existingReservation.isPresent()) {
            return updateReservation(sessionId, productId, existingReservation.get().getQuantity() + quantity);
        }

        Integer availableStock = reservationRepository.calculateAvailableStock(productId, now);
        if (availableStock == null) {
            availableStock = product.getStock();
        }

        if (quantity > availableStock) {
            throw new ConflictException("INSUFFICIENT_STOCK", "有効在庫が不足しています");
        }

        StockReservation reservation = new StockReservation();
        reservation.setProduct(product);
        reservation.setSessionId(sessionId);
        reservation.setQuantity(quantity);
        reservation.setType(ReservationType.TENTATIVE);
        reservation.setExpiresAt(now.plus(RESERVATION_EXPIRY_MINUTES, ChronoUnit.MINUTES));

        reservation = reservationRepository.save(reservation);

        Integer newAvailableStock = reservationRepository.calculateAvailableStock(productId, Instant.now());
        return ReservationDto.fromEntity(reservation, newAvailableStock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationDto updateReservation(String sessionId, Long productId, Integer newQuantity) {
        Instant now = Instant.now();

        StockReservation reservation = reservationRepository.findActiveTentative(sessionId, productId, now)
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "引当が見つかりません"));

        int diff = newQuantity - reservation.getQuantity();

        if (diff > 0) {
            Integer availableStock = reservationRepository.calculateAvailableStock(productId, now);
            if (availableStock == null) {
                availableStock = reservation.getProduct().getStock();
            }
            if (diff > availableStock) {
                throw new ConflictException("INSUFFICIENT_STOCK", "有効在庫が不足しています");
            }
        }

        reservation.setQuantity(newQuantity);
        reservation.setExpiresAt(now.plus(RESERVATION_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        reservation = reservationRepository.save(reservation);

        Integer newAvailableStock = reservationRepository.calculateAvailableStock(productId, Instant.now());
        return ReservationDto.fromEntity(reservation, newAvailableStock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseReservation(String sessionId, Long productId) {
        Instant now = Instant.now();

        StockReservation reservation = reservationRepository.findActiveTentative(sessionId, productId, now)
                .orElse(null);

        if (reservation != null) {
            reservationRepository.delete(reservation);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseAllReservations(String sessionId) {
        Instant now = Instant.now();
        List<StockReservation> reservations = reservationRepository.findAllActiveTentativeBySession(sessionId, now);
        reservationRepository.deleteAll(reservations);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void commitReservations(String sessionId, Order order) {
        Instant now = Instant.now();
        List<StockReservation> tentativeReservations =
                reservationRepository.findAllActiveTentativeBySession(sessionId, now);

        if (tentativeReservations.isEmpty()) {
            throw new BusinessException("NO_RESERVATIONS", "仮引当が存在しません");
        }

        List<StockShortageDetail> shortages = new ArrayList<>();

        for (StockReservation reservation : tentativeReservations) {
            Product product = reservation.getProduct();
            Integer availableStock = reservationRepository.calculateAvailableStock(product.getId(), now);

            if (availableStock == null) {
                availableStock = product.getStock();
            }

            if (reservation.getQuantity() > availableStock) {
                shortages.add(new StockShortageDetail(
                    product.getId(),
                    product.getName(),
                    reservation.getQuantity(),
                    availableStock
                ));
            }
        }

        if (!shortages.isEmpty()) {
            throw new InsufficientStockException("OUT_OF_STOCK", "在庫が不足している商品があります", shortages);
        }

        for (StockReservation reservation : tentativeReservations) {
            Product product = reservation.getProduct();

            product.setStock(product.getStock() - reservation.getQuantity());
            productRepository.save(product);

            reservation.setType(ReservationType.COMMITTED);
            reservation.setOrder(order);
            reservation.setExpiresAt(null);
            reservationRepository.save(reservation);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseCommittedReservations(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BusinessException("ALREADY_CANCELLED", "この注文は既にキャンセルされています");
        }

        if (order.getStatus() == Order.OrderStatus.SHIPPED || order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new BusinessException("ORDER_NOT_CANCELLABLE", "この注文はキャンセルできません");
        }

        List<StockReservation> committedReservations =
                reservationRepository.findByOrderIdAndType(orderId, ReservationType.COMMITTED);

        for (StockReservation reservation : committedReservations) {
            Product product = reservation.getProduct();
            product.setStock(product.getStock() + reservation.getQuantity());
            productRepository.save(product);

            reservationRepository.delete(reservation);
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public AvailabilityDto getAvailableStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        Instant now = Instant.now();

        Integer tentativeReserved = reservationRepository.sumTentativeReserved(productId, now);
        Integer committedReserved = reservationRepository.sumCommittedReserved(productId);
        int availableStock = product.getStock() - tentativeReserved - committedReserved;

        AvailabilityDto dto = new AvailabilityDto();
        dto.setProductId(productId);
        dto.setPhysicalStock(product.getStock());
        dto.setTentativeReserved(tentativeReserved);
        dto.setCommittedReserved(committedReserved);
        dto.setAvailableStock(Math.max(0, availableStock));

        return dto;
    }

    @Override
    public List<InventoryStatusDto> getAllInventoryStatus() {
        List<Product> products = productRepository.findAll();
        Instant now = Instant.now();

        return products.stream().map(product -> {
            Integer tentative = reservationRepository.sumTentativeReserved(product.getId(), now);
            Integer committed = reservationRepository.sumCommittedReserved(product.getId());
            Integer available = product.getStock() - tentative - committed;

            return InventoryStatusDto.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .physicalStock(product.getStock())
                    .tentativeReserved(tentative)
                    .committedReserved(committed)
                    .availableStock(available)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjustment adjustStock(Long productId, Integer quantityDelta, String reason, BoUser admin) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "商品が見つかりません"));

        Integer quantityBefore = product.getStock();
        Integer quantityAfter = quantityBefore + quantityDelta;

        if (quantityAfter < 0) {
            throw new BusinessException("INVALID_STOCK_ADJUSTMENT",
                    "在庫調整後の数量が負になります（現在: " + quantityBefore + ", 調整: " + quantityDelta + "）");
        }

        product.setStock(quantityAfter);
        productRepository.save(product);

        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setProduct(product);
        adjustment.setQuantityBefore(quantityBefore);
        adjustment.setQuantityAfter(quantityAfter);
        adjustment.setQuantityDelta(quantityDelta);
        adjustment.setReason(reason);
        adjustment.setAdjustedBy(admin.getEmail());
        adjustment.setAdjustedAt(Instant.now());

        return inventoryAdjustmentRepository.save(adjustment);
    }

    @Scheduled(fixedRate = 300_000)
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredReservations() {
        reservationRepository.deleteByTypeAndExpiresAtBefore(ReservationType.TENTATIVE, Instant.now());
        log.debug("期限切れの仮引当をクリーンアップしました");
    }

}
