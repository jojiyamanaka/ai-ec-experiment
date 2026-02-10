package com.example.aiec.service;

import com.example.aiec.dto.AvailabilityDto;
import com.example.aiec.dto.ReservationDto;
import com.example.aiec.entity.Order;
import com.example.aiec.entity.Product;
import com.example.aiec.entity.StockReservation;
import com.example.aiec.entity.StockReservation.ReservationType;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ConflictException;
import com.example.aiec.exception.ResourceNotFoundException;
import com.example.aiec.repository.OrderRepository;
import com.example.aiec.repository.ProductRepository;
import com.example.aiec.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 仮引当を作成する
     */
    @Transactional
    public ReservationDto createReservation(String sessionId, Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        LocalDateTime now = LocalDateTime.now();

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
        reservation.setExpiresAt(now.plusMinutes(RESERVATION_EXPIRY_MINUTES));

        reservation = reservationRepository.save(reservation);

        Integer newAvailableStock = reservationRepository.calculateAvailableStock(productId, LocalDateTime.now());
        return ReservationDto.fromEntity(reservation, newAvailableStock);
    }

    /**
     * 仮引当を更新する（カート数量変更時）
     */
    @Transactional
    public ReservationDto updateReservation(String sessionId, Long productId, Integer newQuantity) {
        LocalDateTime now = LocalDateTime.now();

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
        reservation.setExpiresAt(now.plusMinutes(RESERVATION_EXPIRY_MINUTES));
        reservation = reservationRepository.save(reservation);

        Integer newAvailableStock = reservationRepository.calculateAvailableStock(productId, LocalDateTime.now());
        return ReservationDto.fromEntity(reservation, newAvailableStock);
    }

    /**
     * 仮引当を解除する（カートから商品削除時）
     */
    @Transactional
    public void releaseReservation(String sessionId, Long productId) {
        LocalDateTime now = LocalDateTime.now();

        StockReservation reservation = reservationRepository.findActiveTentative(sessionId, productId, now)
                .orElse(null);

        if (reservation != null) {
            reservationRepository.delete(reservation);
        }
    }

    /**
     * セッションの全仮引当を解除する（カートクリア時）
     */
    @Transactional
    public void releaseAllReservations(String sessionId) {
        LocalDateTime now = LocalDateTime.now();
        List<StockReservation> reservations = reservationRepository.findAllActiveTentativeBySession(sessionId, now);
        reservationRepository.deleteAll(reservations);
    }

    /**
     * 仮引当を本引当に変換する（注文確定時）
     * products.stock を減少させる
     */
    @Transactional
    public void commitReservations(String sessionId, Order order) {
        LocalDateTime now = LocalDateTime.now();
        List<StockReservation> tentativeReservations =
                reservationRepository.findAllActiveTentativeBySession(sessionId, now);

        if (tentativeReservations.isEmpty()) {
            throw new BusinessException("NO_RESERVATIONS", "仮引当が存在しません");
        }

        for (StockReservation reservation : tentativeReservations) {
            Product product = reservation.getProduct();

            // 有効在庫の最終チェック（自分の仮引当分は差し引いて考える）
            if (product.getStock() < reservation.getQuantity()) {
                throw new ConflictException("OUT_OF_STOCK", "在庫が不足している商品があります");
            }

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
    @Transactional
    public void releaseCommittedReservations(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

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
    @Transactional(readOnly = true)
    public AvailabilityDto getAvailableStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("ITEM_NOT_FOUND", "商品が見つかりません"));

        LocalDateTime now = LocalDateTime.now();

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
    @Transactional
    public void cleanupExpiredReservations() {
        reservationRepository.deleteByTypeAndExpiresAtBefore(ReservationType.TENTATIVE, LocalDateTime.now());
        log.debug("期限切れの仮引当をクリーンアップしました");
    }

}
