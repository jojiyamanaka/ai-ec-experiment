package com.example.aiec.modules.inventory.adapter.rest;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
import com.example.aiec.modules.inventory.application.port.InventoryQueryPort;
import com.example.aiec.modules.inventory.application.port.InventoryStatusDto;
import com.example.aiec.modules.inventory.domain.entity.InventoryAdjustment;
import com.example.aiec.modules.inventory.domain.repository.InventoryAdjustmentRepository;
import com.example.aiec.modules.product.domain.entity.Product;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BoAdminInventoryControllerContractTest {

    @Mock
    InventoryQueryPort inventoryQuery;

    @Mock
    InventoryCommandPort inventoryCommand;

    @Mock
    InventoryAdjustmentRepository adjustmentRepository;

    @Mock
    BoAuthService boAuthService;

    @Mock
    OutboxEventPublisher outboxEventPublisher;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BoAdminInventoryController controller = new BoAdminInventoryController(
                inventoryQuery,
                inventoryCommand,
                adjustmentRepository,
                boAuthService,
                outboxEventPublisher
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllInventory_withOperator_shouldReturnForbidden() throws Exception {
        when(boAuthService.verifyToken("operator-token")).thenReturn(buildBoUser(PermissionLevel.OPERATOR));

        mockMvc.perform(get("/api/bo/admin/inventory")
                        .header("Authorization", "Bearer operator-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void getAllInventory_withAdmin_shouldReturnInventoryList() throws Exception {
        InventoryStatusDto dto = InventoryStatusDto.builder()
                .productId(1L)
                .productName("商品A")
                .availableStock(10)
                .build();
        when(boAuthService.verifyToken("admin-token")).thenReturn(buildBoUser(PermissionLevel.ADMIN));
        when(inventoryQuery.getAllInventoryStatus()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/bo/admin/inventory")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].productId").value(1));
    }

    @Test
    void adjustStock_shouldReturnAdjustmentResult() throws Exception {
        InventoryAdjustment adjustment = new InventoryAdjustment();
        Product product = new Product();
        product.setId(1L);
        product.setName("商品A");
        adjustment.setProduct(product);
        adjustment.setQuantityBefore(10);
        adjustment.setQuantityAfter(15);
        adjustment.setQuantityDelta(5);
        adjustment.setReason("棚卸調整");

        BoUser admin = buildBoUser(PermissionLevel.ADMIN);
        when(boAuthService.verifyToken("admin-token")).thenReturn(admin);
        when(inventoryCommand.adjustStock(eq(1L), eq(5), eq("棚卸調整"), eq(admin))).thenReturn(adjustment);

        mockMvc.perform(post("/api/bo/admin/inventory/adjust")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 1,
                                  "quantityDelta": 5,
                                  "reason": "棚卸調整"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quantityDelta").value(5));
    }

    @Test
    void getAdjustments_withProductId_shouldUseFilteredRepositoryMethod() throws Exception {
        when(boAuthService.verifyToken("admin-token")).thenReturn(buildBoUser(PermissionLevel.ADMIN));
        when(adjustmentRepository.findByProductIdOrderByAdjustedAtDesc(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/bo/admin/inventory/adjustments")
                        .param("productId", "1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(adjustmentRepository).findByProductIdOrderByAdjustedAtDesc(1L);
    }

    @Test
    void getAdjustments_withoutAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/bo/admin/inventory/adjustments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private BoUser buildBoUser(PermissionLevel permissionLevel) {
        BoUser user = new BoUser();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setPermissionLevel(permissionLevel);
        user.setIsActive(true);
        return user;
    }
}
