package com.example.aiec.modules.purchase.adapter.rest;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.customer.domain.service.AuthService;
import com.example.aiec.modules.purchase.application.port.OrderCommandPort;
import com.example.aiec.modules.purchase.application.port.OrderDto;
import com.example.aiec.modules.purchase.application.port.OrderQueryPort;
import com.example.aiec.modules.purchase.cart.service.CartService;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import com.example.aiec.modules.purchase.adapter.dto.CartDto;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerContractTest {

    @Mock private CartService cartService;
    @Mock private OrderCommandPort orderCommand;
    @Mock private OrderQueryPort orderQuery;
    @Mock private AuthService authService;
    @Mock private BoAuthService boAuthService;
    @Mock private OutboxEventPublisher outboxEventPublisher;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderController(
                        cartService,
                        orderCommand,
                        orderQuery,
                        authService,
                        boAuthService,
                        outboxEventPublisher
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void markShipped_shouldReturnOrderWithAllocationProgress() throws Exception {
        when(boAuthService.verifyToken("token")).thenReturn(adminUser());
        when(orderCommand.markShipped(10L)).thenReturn(buildOrderDto(10L, "SHIPPED", 3, 3));

        mockMvc.perform(post("/api/order/10/mark-shipped")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(10))
                .andExpect(jsonPath("$.data.orderedQuantity").value(3))
                .andExpect(jsonPath("$.data.committedQuantity").value(3));
    }

    @Test
    void retryAllocation_shouldReturnOrderWithAllocationProgress() throws Exception {
        when(boAuthService.verifyToken("token")).thenReturn(adminUser());
        when(orderCommand.retryAllocation(11L)).thenReturn(buildOrderDto(11L, "CONFIRMED", 5, 2));

        mockMvc.perform(post("/api/order/11/allocation/retry")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(11))
                .andExpect(jsonPath("$.data.orderedQuantity").value(5))
                .andExpect(jsonPath("$.data.committedQuantity").value(2));
    }

    @Test
    void createOrder_withInvalidToken_shouldFallbackToGuestOrder() throws Exception {
        when(authService.verifyToken("invalid-token"))
                .thenThrow(new BusinessException("UNAUTHORIZED", "認証が必要です"));
        when(orderCommand.createOrder("session-1", "10", null))
                .thenReturn(buildOrderDto(20L, "PENDING", 2, 0));

        mockMvc.perform(post("/api/order")
                        .header("X-Session-Id", "session-1")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartId": "10"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(20));

        verify(orderCommand).createOrder("session-1", "10", null);
    }

    @Test
    void getOrderHistory_withoutAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/order/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void confirmOrder_withOperator_shouldReturnForbidden() throws Exception {
        when(boAuthService.verifyToken("operator-token")).thenReturn(operatorUser());

        mockMvc.perform(post("/api/order/3/confirm")
                        .header("Authorization", "Bearer operator-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void getCart_shouldReturnCart() throws Exception {
        when(cartService.getOrCreateCart("session-1"))
                .thenReturn(new CartDto(List.of(), 0, BigDecimal.ZERO));

        mockMvc.perform(get("/api/order/cart")
                        .header("X-Session-Id", "session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalQuantity").value(0));

        verify(cartService).getOrCreateCart(eq("session-1"));
    }

    private BoUser adminUser() {
        BoUser boUser = new BoUser();
        boUser.setId(1L);
        boUser.setEmail("admin@example.com");
        boUser.setPermissionLevel(PermissionLevel.ADMIN);
        boUser.setIsActive(true);
        return boUser;
    }

    private BoUser operatorUser() {
        BoUser boUser = new BoUser();
        boUser.setId(2L);
        boUser.setEmail("operator@example.com");
        boUser.setPermissionLevel(PermissionLevel.OPERATOR);
        boUser.setIsActive(true);
        return boUser;
    }

    private OrderDto buildOrderDto(Long orderId, String status, int orderedQuantity, int committedQuantity) {
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderId(orderId);
        orderDto.setOrderNumber("ORD-0000000010");
        orderDto.setItems(List.of());
        orderDto.setTotalPrice(BigDecimal.valueOf(1000));
        orderDto.setOrderedQuantity(orderedQuantity);
        orderDto.setCommittedQuantity(committedQuantity);
        orderDto.setStatus(status);
        orderDto.setCreatedAt("2026-02-20T00:00:00+09:00");
        orderDto.setUpdatedAt("2026-02-20T00:00:00+09:00");
        return orderDto;
    }
}
