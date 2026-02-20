package com.example.aiec.modules.inventory.application.usecase;

import com.example.aiec.modules.inventory.domain.entity.LocationStock;
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
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InventoryUseCase の業務ロジック単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class InventoryUseCaseTest {

    @Mock StockReservationRepository reservationRepository;
    @Mock ProductRepository productRepository;
    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock InventoryAdjustmentRepository inventoryAdjustmentRepository;
    @Mock LocationStockRepository locationStockRepository;
    @Mock SalesLimitRepository salesLimitRepository;
    @Mock OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    InventoryUseCase inventoryUseCase;

    private Product buildProduct(Long id, AllocationType allocationType) {
        Product p = new Product();
        p.setId(id);
        p.setName("商品" + id);
        p.setPrice(BigDecimal.valueOf(1000));
        p.setImage("/img/" + id + ".jpg");
        p.setAllocationType(allocationType);
        p.setIsPublished(true);
        return p;
    }

    private LocationStock buildLocationStock(Product product, int allocatable, int allocated) {
        LocationStock locationStock = new LocationStock();
        locationStock.setProduct(product);
        locationStock.setLocationId(1);
        locationStock.setAvailableQty(allocatable);
        locationStock.setCommittedQty(allocated);
        return locationStock;
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

    private Order buildOrder(Long id, Order.OrderStatus status, Product product, int quantity) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber("ORD-" + id);
        order.setTotalPrice(BigDecimal.valueOf(2000));
        order.setStatus(status);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setCommittedQty(0);
        orderItem.setSubtotal(BigDecimal.valueOf(2000));
        order.addItem(orderItem);
        return order;
    }

    @Test
    void createReservation_realProductWithNoStock_shouldThrowConflictException() {
        Product product = buildProduct(1L, AllocationType.REAL);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(reservationRepository.findActiveTentative(eq("sess"), eq(1L), any())).thenReturn(Optional.empty());
        when(locationStockRepository.findByProductIdAndLocationId(1L, 1)).thenReturn(Optional.empty());
        when(reservationRepository.sumTentativeReserved(eq(1L), any())).thenReturn(0);

        assertThatExceptionOfType(ConflictException.class)
                .isThrownBy(() -> inventoryUseCase.createReservation("sess", 1L, 1))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("INSUFFICIENT_STOCK"));
    }

    @Test
    void commitReservations_noTentativeReservations_shouldThrowBusinessException() {
        when(reservationRepository.findAllActiveTentativeBySession(any(), any())).thenReturn(List.of());

        Product product = buildProduct(1L, AllocationType.REAL);
        Order order = buildOrder(1L, Order.OrderStatus.PENDING, product, 1);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> inventoryUseCase.commitReservations("sess", order))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("NO_RESERVATIONS"));
    }

    @Test
    void commitReservations_realProductInsufficient_shouldThrowInsufficientStockException() {
        Product product = buildProduct(1L, AllocationType.REAL);
        StockReservation reservation = buildTentativeReservation(product, 5);
        Order order = buildOrder(10L, Order.OrderStatus.PENDING, product, 5);

        when(reservationRepository.findAllActiveTentativeBySession(any(), any())).thenReturn(List.of(reservation));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(locationStockRepository.findByProductIdAndLocationIdForUpdate(1L, 1))
                .thenReturn(Optional.of(buildLocationStock(product, 3, 0)));

        assertThatExceptionOfType(InsufficientStockException.class)
                .isThrownBy(() -> inventoryUseCase.commitReservations("sess", order));
    }

    @Test
    void commitReservations_realProductSuccess_shouldIncreaseCommittedQty() {
        Product product = buildProduct(1L, AllocationType.REAL);
        StockReservation reservation = buildTentativeReservation(product, 2);
        Order order = buildOrder(10L, Order.OrderStatus.PENDING, product, 2);
        LocationStock locationStock = buildLocationStock(product, 10, 1);

        when(reservationRepository.findAllActiveTentativeBySession(any(), any())).thenReturn(List.of(reservation));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(locationStockRepository.findByProductIdAndLocationIdForUpdate(1L, 1)).thenReturn(Optional.of(locationStock));

        inventoryUseCase.commitReservations("sess", order);

        assertThat(order.getItems().getFirst().getCommittedQty()).isEqualTo(2);
        assertThat(locationStock.getCommittedQty()).isEqualTo(3);
        verify(locationStockRepository).save(locationStock);
        verify(reservationRepository).delete(reservation);
    }

    @Test
    void releaseCommittedReservations_cancelledOrder_shouldThrowBusinessException() {
        Product product = buildProduct(1L, AllocationType.REAL);
        Order order = buildOrder(10L, Order.OrderStatus.CANCELLED, product, 1);
        when(orderRepository.findByIdWithItems(10L)).thenReturn(Optional.of(order));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> inventoryUseCase.releaseCommittedReservations(10L))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("ALREADY_CANCELLED"));
    }

    @Test
    void releaseCommittedReservations_success_shouldRestoreLocationStockAndCancelOrder() {
        Product product = buildProduct(1L, AllocationType.REAL);
        Order order = buildOrder(10L, Order.OrderStatus.CONFIRMED, product, 2);
        order.getItems().getFirst().setCommittedQty(2);
        LocationStock locationStock = buildLocationStock(product, 8, 3);

        when(orderRepository.findByIdWithItems(10L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(locationStockRepository.findByProductIdAndLocationIdForUpdate(1L, 1)).thenReturn(Optional.of(locationStock));
        when(reservationRepository.findByOrderIdAndType(10L, ReservationType.COMMITTED)).thenReturn(List.of());

        inventoryUseCase.releaseCommittedReservations(10L);

        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        assertThat(order.getItems().getFirst().getCommittedQty()).isEqualTo(0);
        assertThat(locationStock.getCommittedQty()).isEqualTo(1);
        verify(orderRepository).save(order);
        verify(outboxEventPublisher).publish(eq("STOCK_AVAILABILITY_INCREASED"), eq("1"), any());
    }

    @Test
    void releaseCommittedReservations_shippedOrder_shouldThrowBusinessException() {
        Product product = buildProduct(1L, AllocationType.REAL);
        Order order = buildOrder(10L, Order.OrderStatus.SHIPPED, product, 1);
        when(orderRepository.findByIdWithItems(10L)).thenReturn(Optional.of(order));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> inventoryUseCase.releaseCommittedReservations(10L))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("ORDER_NOT_CANCELLABLE"));

        verify(locationStockRepository, never()).save(any());
    }
}
