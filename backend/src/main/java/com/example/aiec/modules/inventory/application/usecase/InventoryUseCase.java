package com.example.aiec.modules.inventory.application.usecase;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.inventory.application.port.AdminItemInventoryDto;
import com.example.aiec.modules.inventory.application.port.AvailabilityDto;
import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
import com.example.aiec.modules.inventory.application.port.InventoryQueryPort;
import com.example.aiec.modules.inventory.application.port.InventoryStatusDto;
import com.example.aiec.modules.inventory.application.port.LocationStockDto;
import com.example.aiec.modules.inventory.application.port.ReservationDto;
import com.example.aiec.modules.inventory.application.port.SalesLimitDto;
import com.example.aiec.modules.inventory.application.port.StockShortageDetail;
import com.example.aiec.modules.inventory.application.port.UpdateItemInventoryRequest;
import com.example.aiec.modules.inventory.domain.entity.InventoryAdjustment;
import com.example.aiec.modules.inventory.domain.entity.LocationStock;
import com.example.aiec.modules.inventory.domain.entity.SalesLimit;
import com.example.aiec.modules.inventory.domain.entity.StockReservation;
import com.example.aiec.modules.inventory.domain.entity.StockReservation.ReservationType;
import com.example.aiec.modules.inventory.domain.repository.InventoryAdjustmentRepository;
import com.example.aiec.modules.inventory.domain.repository.LocationStockRepository;
import com.example.aiec.modules.inventory.domain.repository.SalesLimitRepository;
import com.example.aiec.modules.inventory.domain.repository.StockReservationRepository;
import com.example.aiec.modules.product.domain.entity.AllocationType;
import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.product.domain.repository.ProductRepository;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.entity.OrderItem;
import com.example.aiec.modules.purchase.order.repository.OrderItemRepository;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ConflictException;
import com.example.aiec.modules.shared.exception.InsufficientStockException;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 在庫ユースケース（Port実装）
 */
@Service
@RequiredArgsConstructor
class InventoryUseCase implements InventoryQueryPort, InventoryCommandPort {

    private static final int RESERVATION_EXPIRY_MINUTES = 30;
    private static final int DEFAULT_LOCATION_ID = 1;

    private final StockReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final LocationStockRepository locationStockRepository;
    private final SalesLimitRepository salesLimitRepository;
    private final OutboxEventPublisher outboxEventPublisher;

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

        int availableStock = calculateEffectiveStock(product, now);
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
        return ReservationDto.fromEntity(reservation, calculateEffectiveStock(product, Instant.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationDto updateReservation(String sessionId, Long productId, Integer newQuantity) {
        Instant now = Instant.now();

        StockReservation reservation = reservationRepository.findActiveTentative(sessionId, productId, now)
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "引当が見つかりません"));

        int diff = newQuantity - reservation.getQuantity();
        if (diff > 0) {
            int availableStock = calculateEffectiveStock(reservation.getProduct(), now);
            if (diff > availableStock) {
                throw new ConflictException("INSUFFICIENT_STOCK", "有効在庫が不足しています");
            }
        }

        reservation.setQuantity(newQuantity);
        reservation.setExpiresAt(now.plus(RESERVATION_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        reservation = reservationRepository.save(reservation);

        return ReservationDto.fromEntity(reservation, calculateEffectiveStock(reservation.getProduct(), Instant.now()));
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
        List<StockReservation> tentativeReservations = reservationRepository.findAllActiveTentativeBySession(sessionId, now);

        if (tentativeReservations.isEmpty()) {
            throw new BusinessException("NO_RESERVATIONS", "仮引当が存在しません");
        }

        Map<Long, OrderItem> orderItemsByProductId = order.getItems().stream()
                .collect(Collectors.toMap(item -> item.getProduct().getId(), Function.identity(), (left, right) -> left));
        List<StockShortageDetail> shortages = new ArrayList<>();
        Map<Long, LocationStock> locationStockByProductId = new HashMap<>();

        for (StockReservation reservation : tentativeReservations) {
            Product product = productRepository.findByIdForUpdate(reservation.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));
            int quantity = reservation.getQuantity();

            if (product.getAllocationType() == AllocationType.REAL) {
                LocationStock locationStock = findOrCreateLocationStockForUpdate(product);
                locationStockByProductId.put(product.getId(), locationStock);
                if (quantity > locationStock.remainingQty()) {
                    shortages.add(new StockShortageDetail(product.getId(), product.getName(), quantity, locationStock.remainingQty()));
                }
            } else {
                int remainingFrameQty = calculateFrameRemainingQtyExcludingOrder(product.getId(), order.getId());
                if (quantity > remainingFrameQty) {
                    shortages.add(new StockShortageDetail(product.getId(), product.getName(), quantity, remainingFrameQty));
                }
            }
        }

        if (!shortages.isEmpty()) {
            throw new InsufficientStockException("OUT_OF_STOCK", "在庫が不足している商品があります", shortages);
        }

        for (StockReservation reservation : tentativeReservations) {
            Product product = reservation.getProduct();
            OrderItem orderItem = orderItemsByProductId.get(product.getId());
            if (orderItem == null) {
                continue;
            }

            if (product.getAllocationType() == AllocationType.REAL) {
                LocationStock locationStock = locationStockByProductId.get(product.getId());
                int currentAllocated = orderItem.getAllocatedQty() != null ? orderItem.getAllocatedQty() : 0;
                int allocateQty = reservation.getQuantity();
                orderItem.setAllocatedQty(Math.min(orderItem.getQuantity(), currentAllocated + allocateQty));
                locationStock.setAllocatedQty(locationStock.getAllocatedQty() + allocateQty);
                locationStockRepository.save(locationStock);
            } else {
                orderItem.setAllocatedQty(0);
            }

            reservationRepository.delete(reservation);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseCommittedReservations(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BusinessException("ALREADY_CANCELLED", "この注文は既にキャンセルされています");
        }

        if (order.getStatus() == Order.OrderStatus.SHIPPED || order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new BusinessException("ORDER_NOT_CANCELLABLE", "この注文はキャンセルできません");
        }

        Map<Long, Integer> releasedQtyByProductId = new HashMap<>();
        for (OrderItem orderItem : order.getItems()) {
            int allocatedQty = valueOrZero(orderItem.getAllocatedQty());
            if (allocatedQty > 0) {
                releasedQtyByProductId.merge(orderItem.getProduct().getId(), allocatedQty, Integer::sum);
                orderItem.setAllocatedQty(0);
            }
        }

        for (Map.Entry<Long, Integer> entry : releasedQtyByProductId.entrySet()) {
            Long productId = entry.getKey();
            Integer releasedQty = entry.getValue();
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));
            LocationStock locationStock = findOrCreateLocationStockForUpdate(product);
            int beforeRemaining = locationStock.remainingQty();
            locationStock.setAllocatedQty(Math.max(0, locationStock.getAllocatedQty() - releasedQty));
            locationStockRepository.save(locationStock);
            int afterRemaining = locationStock.remainingQty();
            if (afterRemaining > beforeRemaining) {
                publishStockAvailabilityIncreased(productId, "ORDER_CANCELLED", releasedQty, orderId);
            }
        }

        List<StockReservation> committedReservations = reservationRepository.findByOrderIdAndType(orderId, ReservationType.COMMITTED);
        reservationRepository.deleteAll(committedReservations);

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public AvailabilityDto getAvailableStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        Instant now = Instant.now();
        int tentativeReserved = valueOrZero(reservationRepository.sumTentativeReserved(productId, now));
        int physicalStock;
        int committedReserved;
        int availableStock;

        if (product.getAllocationType() == AllocationType.REAL) {
            LocationStock locationStock = locationStockRepository.findByProductIdAndLocationId(productId, DEFAULT_LOCATION_ID)
                    .orElse(null);
            physicalStock = locationStock != null ? valueOrZero(locationStock.getAllocatableQty()) : 0;
            committedReserved = locationStock != null ? valueOrZero(locationStock.getAllocatedQty()) : 0;
            availableStock = Math.max(0, physicalStock - committedReserved - tentativeReserved);
        } else {
            SalesLimit salesLimit = salesLimitRepository.findByProductId(productId).orElse(null);
            physicalStock = salesLimit != null ? valueOrZero(salesLimit.getSalesLimitTotal()) : 0;
            committedReserved = valueOrZero(orderItemRepository.sumOrderedQuantityByProductAndAllocationType(
                    productId,
                    AllocationType.FRAME,
                    Order.OrderStatus.CANCELLED
            ));
            availableStock = Math.max(0, physicalStock - committedReserved - tentativeReserved);
        }

        AvailabilityDto dto = new AvailabilityDto();
        dto.setProductId(productId);
        dto.setPhysicalStock(physicalStock);
        dto.setTentativeReserved(tentativeReserved);
        dto.setCommittedReserved(committedReserved);
        dto.setAvailableStock(availableStock);
        return dto;
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<InventoryStatusDto> getAllInventoryStatus() {
        List<Product> products = productRepository.findAll();
        Instant now = Instant.now();

        return products.stream().map(product -> {
            Long productId = product.getId();
            int tentative = valueOrZero(reservationRepository.sumTentativeReserved(productId, now));
            LocationStock locationStock = locationStockRepository.findByProductIdAndLocationId(productId, DEFAULT_LOCATION_ID)
                    .orElse(null);
            int physicalStock = locationStock != null ? valueOrZero(locationStock.getAllocatableQty()) : 0;
            int committed = locationStock != null ? valueOrZero(locationStock.getAllocatedQty()) : 0;

            return InventoryStatusDto.builder()
                    .productId(productId)
                    .productName(product.getName())
                    .physicalStock(physicalStock)
                    .tentativeReserved(tentative)
                    .committedReserved(committed)
                    .availableStock(calculateEffectiveStock(product, now))
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjustment adjustStock(Long productId, Integer quantityDelta, String reason, BoUser admin) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "商品が見つかりません"));

        LocationStock locationStock = findOrCreateLocationStockForUpdate(product);
        Integer quantityBefore = valueOrZero(locationStock.getAllocatableQty());
        Integer quantityAfter = quantityBefore + quantityDelta;
        if (quantityAfter < 0) {
            throw new BusinessException("INVALID_STOCK_ADJUSTMENT",
                    "在庫調整後の数量が負になります（現在: " + quantityBefore + ", 調整: " + quantityDelta + "）");
        }

        int remainingBefore = locationStock.remainingQty();
        locationStock.setAllocatableQty(quantityAfter);
        locationStockRepository.save(locationStock);
        int remainingAfter = locationStock.remainingQty();

        if (remainingAfter > remainingBefore) {
            publishStockAvailabilityIncreased(productId, "INVENTORY_ADJUSTED", quantityDelta, null);
        }

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

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public AdminItemInventoryDto getAdminItemInventory(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        LocationStock locationStock = locationStockRepository.findByProductIdAndLocationId(productId, DEFAULT_LOCATION_ID)
                .orElseGet(() -> {
                    LocationStock emptyStock = new LocationStock();
                    emptyStock.setProduct(product);
                    emptyStock.setLocationId(DEFAULT_LOCATION_ID);
                    emptyStock.setAllocatableQty(0);
                    emptyStock.setAllocatedQty(0);
                    return emptyStock;
                });
        SalesLimit salesLimit = salesLimitRepository.findByProductId(productId)
                .orElseGet(() -> {
                    SalesLimit emptyLimit = new SalesLimit();
                    emptyLimit.setProduct(product);
                    emptyLimit.setSalesLimitTotal(0);
                    return emptyLimit;
                });

        int consumedQty = valueOrZero(orderItemRepository.sumOrderedQuantityByProductAndAllocationType(
                productId,
                AllocationType.FRAME,
                Order.OrderStatus.CANCELLED
        ));

        return new AdminItemInventoryDto(
                productId,
                product.getAllocationType(),
                new LocationStockDto(
                        locationStock.getLocationId(),
                        valueOrZero(locationStock.getAllocatableQty()),
                        valueOrZero(locationStock.getAllocatedQty()),
                        locationStock.remainingQty()
                ),
                new SalesLimitDto(
                        valueOrZero(salesLimit.getSalesLimitTotal()),
                        consumedQty,
                        Math.max(0, valueOrZero(salesLimit.getSalesLimitTotal()) - consumedQty)
                )
        );
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Integer calculateEffectiveStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));
        return calculateEffectiveStock(product, Instant.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminItemInventoryDto updateAdminItemInventory(Long productId, UpdateItemInventoryRequest request, BoUser admin) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        LocationStock locationStock = findOrCreateLocationStockForUpdate(product);
        SalesLimit salesLimit = findOrCreateSalesLimitForUpdate(product);
        int remainingBefore = locationStock.remainingQty();

        if (request.getAllocationType() != null) {
            product.setAllocationType(request.getAllocationType());
        }
        if (request.getLocationStock() != null && request.getLocationStock().getAllocatableQty() != null) {
            Integer allocatableQty = request.getLocationStock().getAllocatableQty();
            if (allocatableQty < 0) {
                int current = valueOrZero(locationStock.getAllocatableQty());
                int delta = allocatableQty - current;
                throw new BusinessException("INVALID_STOCK_ADJUSTMENT",
                        "在庫調整後の数量が負になります（現在: " + current + ", 調整: " + delta + "）");
            }
            locationStock.setAllocatableQty(allocatableQty);
        }
        if (request.getSalesLimit() != null && request.getSalesLimit().getSalesLimitTotal() != null) {
            Integer salesLimitTotal = request.getSalesLimit().getSalesLimitTotal();
            if (salesLimitTotal < 0) {
                int current = valueOrZero(salesLimit.getSalesLimitTotal());
                int delta = salesLimitTotal - current;
                throw new BusinessException("INVALID_STOCK_ADJUSTMENT",
                        "在庫調整後の数量が負になります（現在: " + current + ", 調整: " + delta + "）");
            }
            salesLimit.setSalesLimitTotal(salesLimitTotal);
        }

        productRepository.save(product);
        locationStockRepository.save(locationStock);
        salesLimitRepository.save(salesLimit);

        int remainingAfter = locationStock.remainingQty();
        if (remainingAfter > remainingBefore) {
            publishStockAvailabilityIncreased(productId, "INVENTORY_TAB_UPDATED", remainingAfter - remainingBefore, null);
        }

        return getAdminItemInventory(productId);
    }

    private int calculateEffectiveStock(Product product, Instant now) {
        if (product.getAllocationType() == AllocationType.FRAME) {
            return calculateFrameEffectiveStock(product.getId(), now);
        }
        return calculateRealEffectiveStock(product.getId(), now);
    }

    private int calculateRealEffectiveStock(Long productId, Instant now) {
        LocationStock locationStock = locationStockRepository.findByProductIdAndLocationId(productId, DEFAULT_LOCATION_ID)
                .orElse(null);
        int remainingQty = locationStock != null ? locationStock.remainingQty() : 0;
        int tentativeQty = valueOrZero(reservationRepository.sumTentativeReserved(productId, now));
        return Math.max(0, remainingQty - tentativeQty);
    }

    private int calculateFrameEffectiveStock(Long productId, Instant now) {
        SalesLimit salesLimit = salesLimitRepository.findByProductId(productId).orElse(null);
        int salesLimitTotal = salesLimit != null ? valueOrZero(salesLimit.getSalesLimitTotal()) : 0;
        int consumedQty = valueOrZero(orderItemRepository.sumOrderedQuantityByProductAndAllocationType(
                productId,
                AllocationType.FRAME,
                Order.OrderStatus.CANCELLED
        ));
        int tentativeQty = valueOrZero(reservationRepository.sumTentativeReserved(productId, now));
        return Math.max(0, salesLimitTotal - consumedQty - tentativeQty);
    }

    private int calculateFrameRemainingQtyExcludingOrder(Long productId, Long excludedOrderId) {
        SalesLimit salesLimit = salesLimitRepository.findByProductId(productId).orElse(null);
        int salesLimitTotal = salesLimit != null ? valueOrZero(salesLimit.getSalesLimitTotal()) : 0;
        int consumedQty = valueOrZero(orderItemRepository.sumOrderedQuantityByProductAndAllocationTypeExcludingOrder(
                productId,
                AllocationType.FRAME,
                Order.OrderStatus.CANCELLED,
                excludedOrderId
        ));
        return Math.max(0, salesLimitTotal - consumedQty);
    }

    private LocationStock findOrCreateLocationStockForUpdate(Product product) {
        return locationStockRepository.findByProductIdAndLocationIdForUpdate(product.getId(), DEFAULT_LOCATION_ID)
                .orElseGet(() -> {
                    LocationStock created = new LocationStock();
                    created.setProduct(product);
                    created.setLocationId(DEFAULT_LOCATION_ID);
                    created.setAllocatableQty(0);
                    created.setAllocatedQty(0);
                    return locationStockRepository.save(created);
                });
    }

    private SalesLimit findOrCreateSalesLimitForUpdate(Product product) {
        return salesLimitRepository.findByProductIdForUpdate(product.getId())
                .orElseGet(() -> {
                    SalesLimit created = new SalesLimit();
                    created.setProduct(product);
                    created.setSalesLimitTotal(0);
                    return salesLimitRepository.save(created);
                });
    }

    private void publishStockAvailabilityIncreased(Long productId, String reason, Integer increasedBy, Long orderId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("reason", reason);
        payload.put("increasedBy", increasedBy);
        if (orderId != null) {
            payload.put("orderId", orderId);
        }
        outboxEventPublisher.publish("STOCK_AVAILABILITY_INCREASED", String.valueOf(productId), payload);
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }
}
