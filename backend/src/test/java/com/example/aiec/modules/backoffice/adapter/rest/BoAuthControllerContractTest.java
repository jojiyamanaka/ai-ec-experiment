package com.example.aiec.modules.backoffice.adapter.rest;

import com.example.aiec.modules.backoffice.domain.entity.BoAuthToken;
import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.backoffice.domain.service.BoUserService;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.shared.exception.GlobalExceptionHandler;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BoAuthControllerContractTest {

    @Mock
    BoUserService boUserService;

    @Mock
    BoAuthService boAuthService;

    @Mock
    OutboxEventPublisher outboxEventPublisher;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BoAuthController controller = new BoAuthController(boUserService, boAuthService, outboxEventPublisher);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_withUnknownEmail_shouldReturnInvalidCredentials() throws Exception {
        when(boUserService.findByEmail("unknown@example.com"))
                .thenThrow(new ResourceNotFoundException("BO_USER_NOT_FOUND", "BoUserが見つかりません"));

        mockMvc.perform(post("/api/bo-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unknown@example.com",
                                  "password": "invalid"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_withInvalidPassword_shouldReturnInvalidCredentials() throws Exception {
        BoUser boUser = adminUser();
        when(boUserService.findByEmail("admin@example.com")).thenReturn(boUser);
        when(boUserService.verifyPassword(boUser, "invalid")).thenReturn(false);

        mockMvc.perform(post("/api/bo-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@example.com",
                                  "password": "invalid"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void logout_withoutAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/bo-auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void logout_withBearerToken_shouldRevokeToken() throws Exception {
        mockMvc.perform(post("/api/bo-auth/logout")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("ログアウトしました"));

        verify(boAuthService).revokeToken("test-token");
    }

    @Test
    void me_withBearerToken_shouldReturnBoUser() throws Exception {
        when(boAuthService.verifyToken("valid-token")).thenReturn(adminUser());

        mockMvc.perform(get("/api/bo-auth/me")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@example.com"));
    }

    @Test
    void login_success_shouldReturnTokenAndExpiry() throws Exception {
        BoUser boUser = adminUser();
        when(boUserService.findByEmail("admin@example.com")).thenReturn(boUser);
        when(boUserService.verifyPassword(boUser, "password123")).thenReturn(true);

        BoAuthToken token = new BoAuthToken();
        token.setExpiresAt(Instant.parse("2026-12-31T23:59:59Z"));
        when(boAuthService.createToken(boUser)).thenReturn(new BoAuthService.TokenPair("raw-token", token));

        mockMvc.perform(post("/api/bo-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("raw-token"));
    }

    private BoUser adminUser() {
        BoUser boUser = new BoUser();
        boUser.setId(1L);
        boUser.setEmail("admin@example.com");
        boUser.setDisplayName("admin");
        boUser.setIsActive(true);
        boUser.setPermissionLevel(PermissionLevel.ADMIN);
        return boUser;
    }
}
