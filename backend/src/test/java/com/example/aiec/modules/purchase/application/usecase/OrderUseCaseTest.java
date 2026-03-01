package com.example.aiec.modules.purchase.application.usecase;

import com.example.aiec.modules.customer.domain.repository.UserRepository;
import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
import com.example.aiec.modules.inventory.application.service.FrameAllocationService;
import com.example.aiec.modules.purchase.application.port.AdminOrderListResponse;
import com.example.aiec.modules.purchase.application.port.AdminOrderSearchParams;
import com.example.aiec.modules.purchase.application.port.OrderDto;
import com.example.aiec.modules.purchase.cart.repository.CartRepository;
import com.example.aiec.modules.purchase.cart.service.CartService;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.purchase.shipment.repository.ShipmentRepository;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderUseCaseTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartService cartService;
    @Mock private InventoryCommandPort inventoryCommand;
    @Mock private FrameAllocationService frameAllocationService;
    @Mock private UserRepository userRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private OutboxEventPublisher outboxEventPublisher;
    @Mock private MeterRegistry meterRegistry;

    @InjectMocks
    private OrderUseCase orderUseCase;

    @Test
    void getAllOrders_shouldReturnOrdersAndPagination() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-0000000001");
        order.setStatus(Order.OrderStatus.PENDING);
        order.setTotalPrice(BigDecimal.valueOf(1200));
        order.setCreatedAt(Instant.parse("2026-02-20T00:00:00Z"));
        order.setUpdatedAt(Instant.parse("2026-02-20T00:00:00Z"));
        order.setItems(List.of());

        when(orderRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1));

        AdminOrderListResponse response = orderUseCase.getAllOrders(
                new AdminOrderSearchParams(),
                1,
                20
        );

        assertThat(response.getOrders()).hasSize(1);
        OrderDto dto = response.getOrders().get(0);
        assertThat(dto.getOrderNumber()).isEqualTo("ORD-0000000001");
        assertThat(response.getPagination().getPage()).isEqualTo(1);
        assertThat(response.getPagination().getPageSize()).isEqualTo(20);
        assertThat(response.getPagination().getTotalCount()).isEqualTo(1);
    }
}
