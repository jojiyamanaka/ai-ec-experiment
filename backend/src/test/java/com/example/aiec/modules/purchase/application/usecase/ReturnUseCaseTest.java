package com.example.aiec.modules.purchase.application.usecase;

import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.purchase.application.port.CreateReturnRequest;
import com.example.aiec.modules.purchase.application.port.RejectReturnRequest;
import com.example.aiec.modules.purchase.application.port.ReturnListResponse;
import com.example.aiec.modules.purchase.application.port.ReturnShipmentDto;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.entity.OrderItem;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.purchase.shipment.entity.Shipment;
import com.example.aiec.modules.purchase.shipment.repository.ShipmentRepository;
import com.example.aiec.modules.product.domain.entity.Product;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnUseCaseTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private ReturnUseCase returnUseCase;

    @Test
    void createReturn_shouldCreatePendingReturnShipment() {
        Order order = buildDeliveredOrder();
        CreateReturnRequest request = new CreateReturnRequest(
                "商品に傷があった",
                List.of(new CreateReturnRequest.Item(101L, 1))
        );

        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        when(shipmentRepository.existsByOrderIdAndShipmentType(1L, Shipment.ShipmentType.RETURN)).thenReturn(false);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0);
            shipment.setId(50L);
            shipment.setCreatedAt(Instant.parse("2026-03-01T00:00:00Z"));
            shipment.setUpdatedAt(Instant.parse("2026-03-01T00:00:00Z"));
            return shipment;
        });

        ReturnShipmentDto response = returnUseCase.createReturn(1L, 10L, request);

        assertThat(response.getShipmentId()).isEqualTo(50L);
        assertThat(response.getStatus()).isEqualTo("RETURN_PENDING");
        assertThat(response.getReason()).isEqualTo("商品に傷があった");
        assertThat(response.getItems()).hasSize(1);
        verify(outboxEventPublisher).publish(eq("OPERATION_PERFORMED"), eq(null), any());
    }

    @Test
    void rejectReturn_whenApprovedShouldThrowInvalidTransition() {
        Shipment shipment = buildReturnShipment(Shipment.ShipmentStatus.RETURN_APPROVED);

        when(shipmentRepository.findByOrderIdAndShipmentType(1L, Shipment.ShipmentType.RETURN))
                .thenReturn(Optional.of(shipment));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> returnUseCase.rejectReturn(1L, new RejectReturnRequest("対象外")))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo("INVALID_RETURN_STATUS_TRANSITION"));
    }

    @Test
    void getAllReturns_shouldReturnFilteredPagination() {
        Shipment shipment = buildReturnShipment(Shipment.ShipmentStatus.RETURN_PENDING);

        when(shipmentRepository.findByShipmentTypeAndStatusOrderByCreatedAtDesc(
                Shipment.ShipmentType.RETURN,
                Shipment.ShipmentStatus.RETURN_PENDING,
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(shipment), PageRequest.of(0, 20), 1));

        ReturnListResponse response = returnUseCase.getAllReturns(Shipment.ShipmentStatus.RETURN_PENDING, 1, 20);

        assertThat(response.getReturns()).hasSize(1);
        assertThat(response.getPagination().getTotalCount()).isEqualTo(1);
    }

    private Order buildDeliveredOrder() {
        Product product = new Product();
        product.setId(200L);
        product.setName("product-200");
        product.setPrice(BigDecimal.valueOf(1200));
        product.setImage("https://example.com/product.png");
        product.setIsPublished(true);
        product.setIsReturnable(true);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(101L);
        orderItem.setProduct(product);
        orderItem.setProductName("product-200");
        orderItem.setProductPrice(BigDecimal.valueOf(1200));
        orderItem.setQuantity(2);
        orderItem.setSubtotal(BigDecimal.valueOf(2400));

        User user = new User();
        user.setId(10L);
        user.setEmail("member01@example.com");

        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-0000000001");
        order.setUser(user);
        order.setStatus(Order.OrderStatus.DELIVERED);
        order.setDeliveredAt(Instant.now());
        order.setItems(List.of(orderItem));
        order.setCreatedAt(Instant.parse("2026-03-01T00:00:00Z"));
        order.setUpdatedAt(Instant.parse("2026-03-01T00:00:00Z"));

        return order;
    }

    private Shipment buildReturnShipment(Shipment.ShipmentStatus status) {
        Order order = buildDeliveredOrder();
        Shipment shipment = new Shipment();
        shipment.setId(77L);
        shipment.setOrder(order);
        shipment.setShipmentType(Shipment.ShipmentType.RETURN);
        shipment.setStatus(status);
        shipment.setReason("商品に傷があった");
        shipment.setItems(List.of());
        shipment.setCreatedAt(Instant.parse("2026-03-01T00:00:00Z"));
        shipment.setUpdatedAt(Instant.parse("2026-03-01T00:00:00Z"));
        return shipment;
    }
}
