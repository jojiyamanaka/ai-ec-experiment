package com.example.aiec.modules.purchase.application.usecase;

import io.micrometer.core.instrument.MeterRegistry;
import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.customer.domain.repository.UserRepository;
import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
import com.example.aiec.modules.inventory.application.service.FrameAllocationService;
import com.example.aiec.modules.purchase.cart.repository.CartRepository;
import com.example.aiec.modules.purchase.cart.service.CartService;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.purchase.shipment.repository.ShipmentRepository;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderUseCaseOutboxTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartService cartService;

    @Mock
    private InventoryCommandPort inventoryCommand;

    @Mock
    private FrameAllocationService frameAllocationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Mock
    private MeterRegistry meterRegistry;

    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;

    private OrderUseCase orderUseCase;

    @BeforeEach
    void setUp() {
        orderUseCase = new OrderUseCase(
                orderRepository,
                cartRepository,
                cartService,
                inventoryCommand,
                frameAllocationService,
                userRepository,
                shipmentRepository,
                outboxEventPublisher,
                meterRegistry
        );
    }

    @Test
    void confirmOrder_whenPending_shouldPublishOrderConfirmedEvent() {
        Order pendingOrder = buildOrder(100L, Order.OrderStatus.PENDING);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderUseCase.confirmOrder(100L);

        verify(orderRepository).save(pendingOrder);
        verify(outboxEventPublisher).publish(eq("ORDER_CONFIRMED"), eq("100"), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(pendingOrder.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        assertThat(payload.get("orderId")).isEqualTo(100L);
        assertThat(payload.get("orderNumber")).isEqualTo("ORD-0000000100");
        assertThat(payload.get("customerEmail")).isEqualTo("user@example.com");
        assertThat(payload.get("totalPrice")).isEqualTo(BigDecimal.valueOf(5000));
    }

    @Test
    void confirmOrder_whenNotPending_shouldThrowAndNotPublishEvent() {
        Order confirmedOrder = buildOrder(101L, Order.OrderStatus.CONFIRMED);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(confirmedOrder));

        assertThatThrownBy(() -> orderUseCase.confirmOrder(101L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo("INVALID_STATUS_TRANSITION");

        verify(orderRepository, never()).save(any(Order.class));
        verify(outboxEventPublisher, never()).publish(any(), any(), any());
    }

    private Order buildOrder(Long orderId, Order.OrderStatus status) {
        Order order = new Order();
        order.setId(orderId);
        order.setOrderNumber("ORD-0000000100");
        order.setStatus(status);
        order.setTotalPrice(BigDecimal.valueOf(5000));
        order.setCreatedAt(Instant.parse("2026-02-01T00:00:00Z"));
        order.setUpdatedAt(Instant.parse("2026-02-01T00:00:00Z"));

        User user = new User();
        user.setId(77L);
        user.setEmail("user@example.com");
        user.setDisplayName("Test User");
        order.setUser(user);

        return order;
    }
}
