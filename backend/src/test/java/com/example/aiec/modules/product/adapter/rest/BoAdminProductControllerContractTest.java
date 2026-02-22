package com.example.aiec.modules.product.adapter.rest;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
import com.example.aiec.modules.inventory.application.port.InventoryQueryPort;
import com.example.aiec.modules.product.application.port.ProductCategoryDto;
import com.example.aiec.modules.product.application.port.ProductCommandPort;
import com.example.aiec.modules.product.application.port.ProductDto;
import com.example.aiec.modules.product.application.port.ProductListResponse;
import com.example.aiec.modules.product.application.port.ProductQueryPort;
import com.example.aiec.modules.product.domain.entity.AllocationType;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BoAdminProductControllerContractTest {

    @Mock ProductQueryPort productQuery;
    @Mock ProductCommandPort productCommand;
    @Mock BoAuthService boAuthService;
    @Mock OutboxEventPublisher outboxEventPublisher;
    @Mock InventoryQueryPort inventoryQuery;
    @Mock InventoryCommandPort inventoryCommand;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BoAdminProductController(
                        productQuery,
                        productCommand,
                        boAuthService,
                        outboxEventPublisher,
                        inventoryQuery,
                        inventoryCommand))
                .build();
    }

    @Test
    void getAdminProducts_shouldReturnPagedResult() throws Exception {
        ProductDto item = new ProductDto();
        item.setId(1L);
        item.setName("商品A");
        item.setPrice(BigDecimal.valueOf(1000));
        item.setImage("/img/a.jpg");
        item.setDescription("説明");
        item.setAllocationType(AllocationType.REAL);
        item.setEffectiveStock(10);
        item.setIsPublished(true);
        item.setProductCode("P000001");
        item.setCategoryId(10L);
        item.setCategoryName("カテゴリA");
        when(boAuthService.verifyToken("token")).thenReturn(adminUser());
        when(productQuery.getAdminProducts(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new ProductListResponse(List.of(item), 1L, 1, 20));

        mockMvc.perform(get("/api/admin/items")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].productCode").value("P000001"))
                .andExpect(jsonPath("$.data.items[0].categoryName").value("カテゴリA"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void getCategories_shouldReturnCategoryList() throws Exception {
        when(boAuthService.verifyToken("token")).thenReturn(adminUser());
        when(productCommand.getCategories()).thenReturn(List.of(new ProductCategoryDto(10L, "カテゴリA", 1, true)));

        mockMvc.perform(get("/api/admin/item-categories")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].isPublished").value(true));
    }

    @Test
    void createProduct_shouldAcceptScheduleFields() throws Exception {
        ProductDto created = new ProductDto();
        created.setId(2L);
        created.setName("新商品");
        created.setPrice(BigDecimal.valueOf(2000));
        created.setImage("/img/new.jpg");
        created.setDescription("説明");
        created.setAllocationType(AllocationType.FRAME);
        created.setEffectiveStock(5);
        created.setIsPublished(true);
        created.setProductCode("P000002");
        created.setCategoryId(10L);
        created.setCategoryName("カテゴリA");
        created.setPublishStartAt(java.time.Instant.parse("2026-02-20T00:00:00Z"));
        created.setPublishEndAt(java.time.Instant.parse("2026-12-31T23:59:59Z"));
        created.setSaleStartAt(java.time.Instant.parse("2026-02-20T00:00:00Z"));
        created.setSaleEndAt(java.time.Instant.parse("2026-12-30T23:59:59Z"));
        when(boAuthService.verifyToken("token")).thenReturn(adminUser());
        when(productCommand.createProduct(any())).thenReturn(created);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productCode", "P000002");
        payload.put("name", "新商品");
        payload.put("description", "説明");
        payload.put("categoryId", 10);
        payload.put("price", 2000);
        payload.put("allocationType", "FRAME");
        payload.put("isPublished", true);
        payload.put("publishStartAt", "2026-02-20T00:00:00Z");
        payload.put("publishEndAt", "2026-12-31T23:59:59Z");
        payload.put("saleStartAt", "2026-02-20T00:00:00Z");
        payload.put("saleEndAt", "2026-12-30T23:59:59Z");
        payload.put("image", "/img/new.jpg");
        String requestJson = objectMapper.writeValueAsString(payload);

        mockMvc.perform(post("/api/admin/items")
                        .header("Authorization", "Bearer token")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productCode").value("P000002"))
                .andExpect(jsonPath("$.data.publishStartAt").isNumber())
                .andExpect(jsonPath("$.data.saleEndAt").isNumber());
    }

    private BoUser adminUser() {
        BoUser boUser = new BoUser();
        boUser.setId(1L);
        boUser.setEmail("admin@example.com");
        boUser.setPermissionLevel(PermissionLevel.ADMIN);
        boUser.setIsActive(true);
        return boUser;
    }
}
