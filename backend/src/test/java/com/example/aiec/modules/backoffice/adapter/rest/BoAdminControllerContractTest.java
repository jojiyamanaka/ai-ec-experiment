package com.example.aiec.modules.backoffice.adapter.rest;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.customer.domain.service.UserProfileService;
import com.example.aiec.modules.customer.domain.service.UserService;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.customer.domain.repository.UserRepository;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.shared.exception.ConflictException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BoAdminControllerContractTest {

    @Mock
    UserRepository userRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    UserService userService;

    @Mock
    UserProfileService userProfileService;

    @Mock
    BoAuthService boAuthService;

    @Mock
    OutboxEventPublisher outboxEventPublisher;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BoAdminController controller = new BoAdminController(
                userRepository,
                orderRepository,
                userService,
                userProfileService,
                boAuthService,
                outboxEventPublisher
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createMember_duplicateEmail_shouldReturnConflict() throws Exception {
        when(boAuthService.verifyToken("token")).thenReturn(buildAdminUser());
        when(userService.createUserByAdmin(
                anyString(),
                anyString(),
                anyString(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()))
                .thenThrow(new ConflictException("EMAIL_ALREADY_EXISTS", "このメールアドレスは既に登録されています"));

        mockMvc.perform(post("/api/bo/admin/members")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "dup@example.com",
                                  "displayName": "dup-user",
                                  "password": "password1234"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void updateMember_withSystemField_shouldReturnInvalidRequest() throws Exception {
        when(boAuthService.verifyToken("token")).thenReturn(buildAdminUser());

        mockMvc.perform(put("/api/bo/admin/members/10")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passwordHash": "updated"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        verify(userProfileService, never()).updateMemberProfile(eq(10L), any(), any());
    }

    private BoUser buildAdminUser() {
        BoUser user = new BoUser();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setPermissionLevel(PermissionLevel.ADMIN);
        user.setIsActive(true);
        return user;
    }
}
