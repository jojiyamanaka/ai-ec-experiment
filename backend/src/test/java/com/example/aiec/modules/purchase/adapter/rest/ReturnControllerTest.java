package com.example.aiec.modules.purchase.adapter.rest;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.customer.domain.service.AuthService;
import com.example.aiec.modules.purchase.application.port.CreateReturnRequest;
import com.example.aiec.modules.purchase.application.port.ReturnCommandPort;
import com.example.aiec.modules.purchase.application.port.ReturnListResponse;
import com.example.aiec.modules.purchase.application.port.ReturnQueryPort;
import com.example.aiec.modules.purchase.application.port.ReturnShipmentDto;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.GlobalExceptionHandler;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReturnControllerTest {

    @Mock private ReturnCommandPort returnCommand;
    @Mock private ReturnQueryPort returnQuery;
    @Mock private AuthService authService;
    @Mock private BoAuthService boAuthService;
    @Mock private OutboxEventPublisher outboxEventPublisher;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReturnController(
                        returnCommand,
                        returnQuery,
                        authService,
                        boAuthService,
                        outboxEventPublisher
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturn_shouldReturnCreatedShipment() throws Exception {
        when(authService.verifyToken("token")).thenReturn(customer());
        when(returnCommand.createReturn(eq(1L), eq(10L), any(CreateReturnRequest.class)))
                .thenReturn(buildResponse("RETURN_PENDING"));

        mockMvc.perform(post("/api/order/1/return")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "商品に傷があった",
                                  "items": [
                                    { "orderItemId": 101, "quantity": 1 }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RETURN_PENDING"));
    }

    @Test
    void getAllReturns_withOperatorShouldReturnForbidden() throws Exception {
        when(boAuthService.verifyToken("token")).thenReturn(operator());

        mockMvc.perform(get("/api/return")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void getReturnByOrderId_withAdminShouldReturnShipment() throws Exception {
        when(authService.verifyToken("token")).thenThrow(new BusinessException("INVALID_TOKEN", "認証が必要です"));
        when(boAuthService.verifyToken("token")).thenReturn(admin());
        when(returnQuery.getReturnByOrderId(2L, null)).thenReturn(buildResponse("RETURN_APPROVED"));

        mockMvc.perform(get("/api/order/2/return")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RETURN_APPROVED"));
    }

    private User customer() {
        User user = new User();
        user.setId(10L);
        user.setEmail("member01@example.com");
        return user;
    }

    private BoUser admin() {
        BoUser boUser = new BoUser();
        boUser.setId(1L);
        boUser.setEmail("admin@example.com");
        boUser.setPermissionLevel(PermissionLevel.ADMIN);
        boUser.setIsActive(true);
        return boUser;
    }

    private BoUser operator() {
        BoUser boUser = new BoUser();
        boUser.setId(2L);
        boUser.setEmail("operator@example.com");
        boUser.setPermissionLevel(PermissionLevel.OPERATOR);
        boUser.setIsActive(true);
        return boUser;
    }

    private ReturnShipmentDto buildResponse(String status) {
        return new ReturnShipmentDto(
                1L,
                1L,
                "ORD-0000000001",
                status,
                "返品待ち",
                "商品に傷があった",
                null,
                List.of(),
                "2026-03-01T09:00:00+09:00",
                "2026-03-01T09:00:00+09:00"
        );
    }
}
