package com.example.aiec.integration;

import com.example.aiec.modules.shared.dto.ApiResponse;
import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.customer.domain.service.AuthService;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.backoffice.domain.service.BoUserService;
import com.example.aiec.modules.customer.domain.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BoAuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private BoUserService boUserService;

    @Autowired
    private AuthService authService;

    @Autowired
    private BoAuthService boAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    private String customerToken;
    private String boToken;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();

        User customer = userService.createUser(
                "customer-" + suffix + "@example.com",
                "Customer",
                "password"
        );
        customerToken = authService.createToken(customer).getRawToken();

        BoUser boUser = boUserService.createBoUser(
                "admin-" + suffix + "@example.com",
                "Admin",
                "admin123",
                PermissionLevel.ADMIN
        );
        boToken = boAuthService.createToken(boUser).getRawToken();
    }

    /**
     * 1. 顧客トークンで /api/bo/** にアクセス → 401/403
     */
    @Test
    void customerTokenCannotAccessBoApi() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/bo/admin/members")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().is4xxClientError())
                .andReturn();

        int status = result.getResponse().getStatus();
        assertTrue(status == 401 || status == 403);
    }

    /**
     * 2. BoUser トークンで /api/** にアクセス → 挙動確認
     */
    @Test
    void boUserTokenCanAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/bo/admin/members")
                        .header("Authorization", "Bearer " + boToken))
                .andExpect(status().isOk());
    }

    /**
     * 3. トークンなしでアクセス → 401
     */
    @Test
    void missingTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/bo/admin/members"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 4. 失効トークンでアクセス → 401
     */
    @Test
    void revokedTokenReturnsUnauthorized() throws Exception {
        boAuthService.revokeToken(boToken);

        MvcResult result = mockMvc.perform(get("/api/bo/admin/members")
                        .header("Authorization", "Bearer " + boToken))
                .andExpect(status().isUnauthorized())
                .andReturn();

        ApiResponse<Void> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
        Map<String, Object> error = objectMapper.convertValue(response.getError(), new TypeReference<>() {
        });
        assertEquals("TOKEN_REVOKED", error.get("code"));
    }
}
