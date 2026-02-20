package com.example.aiec.modules.purchase.application.usecase;

import com.example.aiec.modules.customer.domain.repository.UserRepository;
import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
import com.example.aiec.modules.inventory.application.service.FrameAllocationService;
import com.example.aiec.modules.purchase.application.port.OrderDto;
import com.example.aiec.modules.purchase.cart.entity.Cart;
import com.example.aiec.modules.purchase.cart.entity.CartItem;
import com.example.aiec.modules.purchase.cart.repository.CartRepository;
import com.example.aiec.modules.purchase.cart.service.CartService;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.InsufficientStockException;
import com.example.aiec.modules.shared.exception.ItemNotAvailableException;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.inventory.application.port.StockShortageDetail;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OrderUseCase の状態遷移テスト。
 * OrderUseCase はパッケージプライベートのため、同一パッケージに配置。
 */
@ExtendWith(MockitoExtension.class)
class OrderUseCaseStateTransitionTest {

    @Mock OrderRepository orderRepository;
    @Mock CartRepository cartRepository;
    @Mock CartService cartService;
    @Mock InventoryCommandPort inventoryCommand;
    @Mock FrameAllocationService frameAllocationService;
    @Mock UserRepository userRepository;
    @Mock OutboxEventPublisher outboxEventPublisher;

    private OrderUseCase orderUseCase;

    @BeforeEach
    void setUp() {
        // Spring コンテキスト外でも private な @PostConstruct を明示的に実行してメトリクスを初期化
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        orderUseCase = new OrderUseCase(
                orderRepository, cartRepository, cartService,
                inventoryCommand, frameAllocationService, userRepository, outboxEventPublisher, meterRegistry);
        ReflectionTestUtils.invokeMethod(orderUseCase, "initMetrics");
    }

    // ── ヘルパー ─────────────────────────────────────────────────────────────

    private Order buildOrder(Long id, Order.OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber("ORD-" + String.format("%010d", id));
        order.setTotalPrice(BigDecimal.valueOf(3000));
        order.setStatus(status);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        return order;
    }

    private Cart buildCart(String sessionId, Product product, Integer quantity) {
        Cart cart = new Cart();
        cart.setSessionId(sessionId);
        cart.setCreatedAt(Instant.now());
        cart.setUpdatedAt(Instant.now());

        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cart.addItem(cartItem);
        return cart;
    }

    private Product buildProduct(Long id, boolean isPublished, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName("product-" + id);
        product.setPrice(BigDecimal.valueOf(1000));
        product.setImage("https://example.com/p.png");
        product.setDescription("desc");
        product.setStock(stock);
        product.setIsPublished(isPublished);
        return product;
    }

    // ── createOrder ────────────────────────────────────────────────────────

    @Test
    void createOrder_emptyCart_shouldThrowBusinessExceptionWithCartEmpty() {
        Cart cart = new Cart();
        cart.setSessionId("session-1");
        cart.setCreatedAt(Instant.now());
        cart.setUpdatedAt(Instant.now());

        when(cartRepository.findBySessionId("session-1")).thenReturn(Optional.of(cart));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> orderUseCase.createOrder("session-1", "session-1", null))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("CART_EMPTY"));

        verify(orderRepository, never()).save(any(Order.class));
        verify(inventoryCommand, never()).commitReservations(any(), any(Order.class));
    }

    @Test
    void createOrder_withUnpublishedProduct_shouldThrowItemNotAvailableException() {
        Product unpublishedProduct = buildProduct(10L, false, 5);
        Cart cart = buildCart("session-2", unpublishedProduct, 1);
        when(cartRepository.findBySessionId("session-2")).thenReturn(Optional.of(cart));

        assertThatExceptionOfType(ItemNotAvailableException.class)
                .isThrownBy(() -> orderUseCase.createOrder("session-2", "session-2", null))
                .satisfies(ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("ITEM_NOT_AVAILABLE");
                    assertThat(ex.getDetails()).hasSize(1);
                    assertThat(ex.getDetails().get(0).getProductId()).isEqualTo(10L);
                    assertThat(ex.getDetails().get(0).getProductName()).isEqualTo("product-10");
                });

        verify(orderRepository, never()).save(any(Order.class));
        verify(inventoryCommand, never()).commitReservations(any(), any(Order.class));
    }

    @Test
    void createOrder_whenCommitReservationsFails_shouldThrowInsufficientStockExceptionAndNotClearCart() {
        Product publishedProduct = buildProduct(20L, true, 1);
        Cart cart = buildCart("session-3", publishedProduct, 2);
        when(cartRepository.findBySessionId("session-3")).thenReturn(Optional.of(cart));
        when(orderRepository.getNextOrderNumberSequence()).thenReturn(200L);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new InsufficientStockException(
                "OUT_OF_STOCK",
                "在庫が不足しています",
                List.of(new StockShortageDetail(20L, "product-20", 2, 1))
        )).when(inventoryCommand).commitReservations(eq("session-3"), any(Order.class));

        assertThatExceptionOfType(InsufficientStockException.class)
                .isThrownBy(() -> orderUseCase.createOrder("session-3", "session-3", null))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("OUT_OF_STOCK"));

        verify(orderRepository).save(any(Order.class));
        verify(inventoryCommand).commitReservations(eq("session-3"), any(Order.class));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    // ── confirmOrder ────────────────────────────────────────────────────────

    @Test
    void confirmOrder_pendingOrder_shouldTransitionToConfirmed() {
        Order order = buildOrder(1L, Order.OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDto result = orderUseCase.confirmOrder(1L);

        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        verify(outboxEventPublisher).publish(eq("ORDER_CONFIRMED"), any(), any());
    }

    @Test
    void confirmOrder_nonPendingOrder_shouldThrowBusinessException() {
        Order order = buildOrder(1L, Order.OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> orderUseCase.confirmOrder(1L))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("INVALID_STATUS_TRANSITION"));
    }

    // ── markShipped ────────────────────────────────────────────────────────

    @Test
    void markShipped_preparingShipmentOrder_shouldTransitionToShipped() {
        Order order = buildOrder(2L, Order.OrderStatus.PREPARING_SHIPMENT);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDto result = orderUseCase.markShipped(2L);

        assertThat(result.getStatus()).isEqualTo("SHIPPED");
    }

    @Test
    void markShipped_pendingOrder_shouldThrowBusinessException() {
        Order order = buildOrder(2L, Order.OrderStatus.PENDING);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> orderUseCase.markShipped(2L))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("INVALID_STATUS_TRANSITION"));
    }

    // ── deliverOrder ───────────────────────────────────────────────────────

    @Test
    void deliverOrder_shippedOrder_shouldTransitionToDelivered() {
        Order order = buildOrder(3L, Order.OrderStatus.SHIPPED);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDto result = orderUseCase.deliverOrder(3L);

        assertThat(result.getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void deliverOrder_confirmedOrder_shouldThrowBusinessException() {
        Order order = buildOrder(3L, Order.OrderStatus.CONFIRMED);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> orderUseCase.deliverOrder(3L))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("INVALID_STATUS_TRANSITION"));
    }
}
