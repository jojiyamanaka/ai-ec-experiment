package com.example.aiec.modules.customer.adapter.rest;

import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.customer.domain.service.AuthService;
import com.example.aiec.modules.customer.domain.service.UserProfileService;
import com.example.aiec.modules.customer.domain.service.UserService;
import com.example.aiec.modules.shared.exception.ForbiddenException;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerContractTest {

    @Mock
    UserService userService;

    @Mock
    AuthService authService;

    @Mock
    UserProfileService userProfileService;

    @Mock
    OutboxEventPublisher outboxEventPublisher;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(userService, authService, userProfileService, outboxEventPublisher);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updateMyProfile_withDisallowedField_shouldReturnInvalidRequest() throws Exception {
        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "updated-name",
                                  "memberRank": "GOLD"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        verify(userProfileService, never()).updateMyProfile(anyLong(), any());
    }

    @Test
    void updateMyAddress_otherMemberAddress_shouldReturnForbidden() throws Exception {
        when(authService.verifyToken("token")).thenReturn(buildUser(1L));
        when(userProfileService.updateMyAddress(eq(1L), eq(10L), any()))
                .thenThrow(new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません"));

        mockMvc.perform(put("/api/auth/me/addresses/10")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientName": "田中 太郎",
                                  "postalCode": "1000001",
                                  "prefecture": "東京都",
                                  "city": "千代田区",
                                  "addressLine1": "千代田1-1"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@example.com");
        user.setDisplayName("user");
        return user;
    }
}
