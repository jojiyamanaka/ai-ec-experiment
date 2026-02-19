package com.example.aiec.modules.inventory.application.usecase;

import com.example.aiec.modules.inventory.domain.entity.StockReservation;
import com.example.aiec.modules.inventory.domain.entity.StockReservation.ReservationType;
import com.example.aiec.modules.inventory.domain.repository.InventoryAdjustmentRepository;
import com.example.aiec.modules.inventory.domain.repository.StockReservationRepository;
import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.product.domain.repository.ProductRepository;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * InventoryUseCase の業務ロジック単体テスト。
 * InventoryUseCase はパッケージプライベートのため、同一パッケージに配置。
 */
@ExtendWith(MockitoExtension.class)
class InventoryUseCaseTest {

    @Mock StockReservationRepository reservationRepository;
    @Mock ProductRepository productRepository;
    @Mock OrderRepository orderRepository;
    @Mock InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @InjectMocks
    InventoryUseCase inventoryUseCase;

    // ── ヘルパー ─────────────────────────────────────────────────────────────

    private Product buildProduct(Long id, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName("商品" + id);
        p.setPrice(BigDecimal.valueOf(1000));
        p.setImage("/img/" + id + ".jpg");
        p.setStock(stock);
        p.setIsPublished(true);
        return p;
    }

    private StockReservation buildTentativeReservation(Product product, int quantity) {
        StockReservation r = new StockReservation();
        r.setProduct(product);
        r.setSessionId("sess");
        r.setQuantity(quantity);
        r.setType(ReservationType.TENTATIVE);
        r.setExpiresAt(Instant.now().plusSeconds(1800));
        return r;
    }

    private Order buildOrder(Long id, Order.OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber("ORD-" + id);
        order.setTotalPrice(BigDecimal.valueOf(2000));
        order.setStatus(status);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        return order;
    }

    // ── commitReservations: 仮引当なし ──────────────────────────────────────

    @Test
    void commitReservations_noTentativeReservations_shouldThrowBusinessException() {
        when(reservationRepository.findAllActiveTentativeBySession(any(), any()))
                .thenReturn(List.of());

        Order order = buildOrder(1L, Order.OrderStatus.PENDING);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> inventoryUseCase.commitReservations("sess", order))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("NO_RESERVATIONS"));
    }

    // ── commitReservations: 在庫不足 ──────────────────────────────────────────

    @Test
    void commitReservations_insufficientStock_shouldThrowInsufficientStockException() {
        Product product = buildProduct(1L, 10);
        StockReservation reservation = buildTentativeReservation(product, 5);

        when(reservationRepository.findAllActiveTentativeBySession(any(), any()))
                .thenReturn(List.of(reservation));
        when(reservationRepository.calculateAvailableStock(eq(1L), any()))
                .thenReturn(3); // 引当数量5 > 有効在庫3

        Order order = buildOrder(1L, Order.OrderStatus.PENDING);

        assertThatExceptionOfType(InsufficientStockException.class)
                .isThrownBy(() -> inventoryUseCase.commitReservations("sess", order));
    }

    // ── commitReservations: 正常本引当 ───────────────────────────────────────

    @Test
    void commitReservations_success_shouldDecrementStockAndCommitReservation() {
        Product product = buildProduct(1L, 10);
        StockReservation reservation = buildTentativeReservation(product, 2);

        when(reservationRepository.findAllActiveTentativeBySession(any(), any()))
                .thenReturn(List.of(reservation));
        when(reservationRepository.calculateAvailableStock(eq(1L), any()))
                .thenReturn(10); // 十分な在庫

        Order order = buildOrder(1L, Order.OrderStatus.PENDING);

        inventoryUseCase.commitReservations("sess", order);

        assertThat(product.getStock()).isEqualTo(8); // 10 - 2
        assertThat(reservation.getType()).isEqualTo(ReservationType.COMMITTED);
        assertThat(reservation.getOrder()).isEqualTo(order);
        assertThat(reservation.getExpiresAt()).isNull();
        verify(productRepository).save(product);
        verify(reservationRepository).save(reservation);
    }

    // ── releaseCommittedReservations: CANCELLED注文 ─────────────────────────

    @Test
    void releaseCommittedReservations_alreadyCancelled_shouldThrowBusinessException() {
        Order order = buildOrder(10L, Order.OrderStatus.CANCELLED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> inventoryUseCase.releaseCommittedReservations(10L))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("ALREADY_CANCELLED"));
    }

    // ── releaseCommittedReservations: SHIPPED注文 ──────────────────────────

    @Test
    void releaseCommittedReservations_shippedOrder_shouldThrowBusinessException() {
        Order order = buildOrder(10L, Order.OrderStatus.SHIPPED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> inventoryUseCase.releaseCommittedReservations(10L))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("ORDER_NOT_CANCELLABLE"));
    }

    // ── releaseCommittedReservations: 正常キャンセル ─────────────────────────

    @Test
    void releaseCommittedReservations_success_shouldRestoreStockAndCancelOrder() {
        Product product = buildProduct(1L, 8);
        StockReservation reservation = new StockReservation();
        reservation.setProduct(product);
        reservation.setQuantity(2);
        reservation.setType(ReservationType.COMMITTED);

        Order order = buildOrder(10L, Order.OrderStatus.CONFIRMED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(reservationRepository.findByOrderIdAndType(10L, ReservationType.COMMITTED))
                .thenReturn(List.of(reservation));

        inventoryUseCase.releaseCommittedReservations(10L);

        assertThat(product.getStock()).isEqualTo(10); // 8 + 2
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        verify(productRepository).save(product);
        verify(reservationRepository).delete(reservation);
        verify(orderRepository).save(order);
    }
}
