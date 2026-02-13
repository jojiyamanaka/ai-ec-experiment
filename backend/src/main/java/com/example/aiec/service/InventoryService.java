package com.example.aiec.service;

import com.example.aiec.dto.AvailabilityDto;
import com.example.aiec.dto.InventoryStatusDto;
import com.example.aiec.dto.ReservationDto;
import com.example.aiec.dto.StockShortageDetail;
import com.example.aiec.entity.BoUser;
import com.example.aiec.entity.InventoryAdjustment;
import com.example.aiec.entity.Order;
import com.example.aiec.entity.Product;
import com.example.aiec.entity.StockReservation;
import com.example.aiec.entity.StockReservation.ReservationType;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ConflictException;
import com.example.aiec.exception.InsufficientStockException;
import com.example.aiec.exception.ResourceNotFoundException;
import com.example.aiec.repository.InventoryAdjustmentRepository;
import com.example.aiec.repository.OrderRepository;
import com.example.aiec.repository.ProductRepository;
import com.example.aiec.repository.StockReservationRepository;
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
 * 在庫引当サービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private static final int RESERVATION_EXPIRY_MINUTES = 30;

    private final StockReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;

    /**
     * 仮引当を作成する
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public ReservationDto createReservation(String sessionId, Long productId, Integer quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        Instant now = Instant.now();

        // 既存の仮引当を検索
        var existingReservation = reservationRepository.findActiveTentative(sessionId, productId, now);

        if (existingReservation.isPresent()) {
            // 既存の仮引当がある場合は数量を加算して更新
            return updateReservation(sessionId, productId, existingReservation.get().getQuantity() + quantity);
        }

        // 有効在庫チェック
        Integer availableStock = reservationRepository.calculateAvailableStock(productId, now);
        if (availableStock == null) {
            availableStock = product.getStock();
        }

        if (quantity > availableStock) {
            throw new ConflictException("INSUFFICIENT_STOCK", "有効在庫が不足しています");
        }

        // 仮引当を作成
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

    @Transactional(
            rollbackFor = Exception.class,
            isolation = Isolation.REPEATABLE_READ
    )
    public void reserveTentative(Long productId, int quantity, String sessionId, Long userId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "商品が見つかりません"));

        Integer available = reservationRepository.calculateAvailableStock(productId, Instant.now());
        if (available == null) {
            available = product.getStock();
        }
        if (available < quantity) {
            throw new BusinessException("INSUFFICIENT_STOCK", "在庫が不足しています");
        }

        StockReservation reservation = new StockReservation();
        reservation.setProduct(product);
        reservation.setQuantity(quantity);
        reservation.setSessionId(sessionId);
        reservation.setUserId(userId);
        reservation.setType(ReservationType.TENTATIVE);
        reservation.setExpiresAt(Instant.now().plus(RESERVATION_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        reservationRepository.save(reservation);
    }

    /**
     * 仮引当を更新する（カート数量変更時）
     */
    @Transactional(rollbackFor = Exception.class)
    public ReservationDto updateReservation(String sessionId, Long productId, Integer newQuantity) {
        Instant now = Instant.now();

        StockReservation reservation = reservationRepository.findActiveTentative(sessionId, productId, now)
                .orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "引当が見つかりません"));

        int diff = newQuantity - reservation.getQuantity();

        if (diff > 0) {
            // 増加する場合、差分に対して有効在庫チェック
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

    /**
     * 仮引当を解除する（カートから商品削除時）
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseReservation(String sessionId, Long productId) {
        Instant now = Instant.now();

        StockReservation reservation = reservationRepository.findActiveTentative(sessionId, productId, now)
                .orElse(null);

        if (reservation != null) {
            reservationRepository.delete(reservation);
        }
    }

    /**
     * セッションの全仮引当を解除する（カートクリア時）
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseAllReservations(String sessionId) {
        Instant now = Instant.now();
        List<StockReservation> reservations = reservationRepository.findAllActiveTentativeBySession(sessionId, now);
        reservationRepository.deleteAll(reservations);
    }

    /**
     * 仮引当を本引当に変換する（注文確定時）
     * products.stock を減少させる
     */
    @Transactional(rollbackFor = Exception.class)
    public void commitReservations(String sessionId, Order order) {
        Instant now = Instant.now();
        List<StockReservation> tentativeReservations =
                reservationRepository.findAllActiveTentativeBySession(sessionId, now);

        if (tentativeReservations.isEmpty()) {
            throw new BusinessException("NO_RESERVATIONS", "仮引当が存在しません");
        }

        // 在庫不足商品を収集するリスト
        List<StockShortageDetail> shortages = new ArrayList<>();

        // まず全商品の在庫チェックを実施
        for (StockReservation reservation : tentativeReservations) {
            Product product = reservation.getProduct();
            Integer availableStock = reservationRepository.calculateAvailableStock(product.getId(), now);

            if (availableStock == null) {
                availableStock = product.getStock();
            }

            // 有効在庫の最終チェック
            if (reservation.getQuantity() > availableStock) {
                shortages.add(new StockShortageDetail(
                    product.getId(),
                    product.getName(),
                    reservation.getQuantity(),
                    availableStock
                ));
            }
        }

        // 在庫不足商品がある場合は詳細情報付きで例外をスロー
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException("OUT_OF_STOCK", "在庫が不足している商品があります", shortages);
        }

        // 在庫が十分な場合、本引当を実行
        for (StockReservation reservation : tentativeReservations) {
            Product product = reservation.getProduct();

            // stock を減少させる
            product.setStock(product.getStock() - reservation.getQuantity());
            productRepository.save(product);

            // 仮引当 → 本引当に変換
            reservation.setType(ReservationType.COMMITTED);
            reservation.setOrder(order);
            reservation.setExpiresAt(null);
            reservationRepository.save(reservation);
        }
    }

    /**
     * 本引当を解除する（注文キャンセル時）
     * products.stock を戻す
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseCommittedReservations(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

        // 既にキャンセル済みチェックを追加
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BusinessException("ALREADY_CANCELLED", "この注文は既にキャンセルされています");
        }

        // キャンセル可能かチェック
        if (order.getStatus() == Order.OrderStatus.SHIPPED || order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new BusinessException("ORDER_NOT_CANCELLABLE", "この注文はキャンセルできません");
        }

        List<StockReservation> committedReservations =
                reservationRepository.findByOrderIdAndType(orderId, ReservationType.COMMITTED);

        for (StockReservation reservation : committedReservations) {
            // stock を戻す
            Product product = reservation.getProduct();
            product.setStock(product.getStock() + reservation.getQuantity());
            productRepository.save(product);

            // 本引当レコードを削除
            reservationRepository.delete(reservation);
        }

        // 注文ステータスをキャンセルに変更
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    /**
     * 有効在庫を取得する
     */
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

    /**
     * 期限切れの仮引当を定期的に削除する（5分ごと）
     */
    @Scheduled(fixedRate = 300_000)
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredReservations() {
        reservationRepository.deleteByTypeAndExpiresAtBefore(ReservationType.TENTATIVE, Instant.now());
        log.debug("期限切れの仮引当をクリーンアップしました");
    }

    /**
     * 全商品の在庫状況を一括取得（バッチ処理版）
     * JOIN + GROUP BY で効率的に取得
     */
    public List<InventoryStatusDto> getAllInventoryStatus() {
        List<Product> products = productRepository.findAll();
        Instant now = Instant.now();

        return products.stream().map(product -> {
            // 仮引当数（有効期限内のみ）
            Integer tentative = reservationRepository.sumTentativeReserved(product.getId(), now);

            // 本引当数
            Integer committed = reservationRepository.sumCommittedReserved(product.getId());

            // 有効在庫計算
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

    /**
     * 在庫調整（差分方式）
     * @param productId 商品ID
     * @param quantityDelta 増減量（正: 増加、負: 減少）
     * @param reason 調整理由
     * @param admin 実施管理者
     * @return 調整履歴
     */
    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjustment adjustStock(Long productId, Integer quantityDelta, String reason, BoUser admin) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "商品が見つかりません"));

        Integer quantityBefore = product.getStock();
        Integer quantityAfter = quantityBefore + quantityDelta;

        // 在庫が負にならないようバリデーション
        if (quantityAfter < 0) {
            throw new BusinessException("INVALID_STOCK_ADJUSTMENT",
                    "在庫調整後の数量が負になります（現在: " + quantityBefore + ", 調整: " + quantityDelta + "）");
        }

        // 在庫更新
        product.setStock(quantityAfter);
        productRepository.save(product);

        // 調整履歴記録
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

}
