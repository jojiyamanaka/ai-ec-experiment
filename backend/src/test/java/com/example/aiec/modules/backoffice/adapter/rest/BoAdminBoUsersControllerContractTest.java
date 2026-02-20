package com.example.aiec.modules.backoffice.adapter.rest;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.backoffice.domain.service.BoUserService;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BoAdminBoUsersControllerContractTest {

    @Mock
    BoUserService boUserService;

    @Mock
    BoAuthService boAuthService;

    @Mock
    OutboxEventPublisher outboxEventPublisher;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BoAdminBoUsersController controller = new BoAdminBoUsersController(boUserService, boAuthService, outboxEventPublisher);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getBoUsers_withOperator_shouldReturnForbidden() throws Exception {
        when(boAuthService.verifyToken("operator-token")).thenReturn(buildBoUser(PermissionLevel.OPERATOR));

        mockMvc.perform(get("/api/bo/admin/bo-users")
                        .header("Authorization", "Bearer operator-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void getBoUsers_withAdmin_shouldReturnList() throws Exception {
        when(boAuthService.verifyToken("admin-token")).thenReturn(buildBoUser(PermissionLevel.ADMIN));
        when(boUserService.findAll()).thenReturn(List.of(buildBoUser(PermissionLevel.ADMIN)));

        mockMvc.perform(get("/api/bo/admin/bo-users")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value("admin@example.com"));
    }

    @Test
    void createBoUser_withoutDisplayName_shouldFallbackToUsername() throws Exception {
        BoUser actor = buildBoUser(PermissionLevel.ADMIN);
        BoUser created = buildBoUser(PermissionLevel.ADMIN);
        created.setDisplayName("ops-user");
        created.setEmail("ops@example.com");

        when(boAuthService.verifyToken("admin-token")).thenReturn(actor);
        when(boUserService.createBoUser(eq("ops@example.com"), eq("ops-user"), eq("password123"), eq(PermissionLevel.ADMIN)))
                .thenReturn(created);

        mockMvc.perform(post("/api/bo/admin/bo-users")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ops-user",
                                  "email": "ops@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("ops-user"));

        verify(boUserService).createBoUser("ops@example.com", "ops-user", "password123", PermissionLevel.ADMIN);
        verify(outboxEventPublisher).publish(eq("OPERATION_PERFORMED"), eq(null), any());
    }

    @Test
    void createBoUser_withoutAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/bo/admin/bo-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ops@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private BoUser buildBoUser(PermissionLevel permissionLevel) {
        BoUser user = new BoUser();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setDisplayName("admin");
        user.setPermissionLevel(permissionLevel);
        user.setIsActive(true);
        return user;
    }
}
