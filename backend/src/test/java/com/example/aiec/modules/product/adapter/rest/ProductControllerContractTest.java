package com.example.aiec.modules.product.adapter.rest;

import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.product.application.port.ProductCommandPort;
import com.example.aiec.modules.product.application.port.ProductDto;
import com.example.aiec.modules.product.application.port.ProductListResponse;
import com.example.aiec.modules.product.application.port.ProductQueryPort;
import com.example.aiec.modules.product.domain.entity.AllocationType;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ProductController のレスポンス形式（コントラクト）テスト。
 * standaloneSetup を使い、Spring Security・JPA への依存なしで動作する。
 * フロントエンドが依存する JSON フィールド名の変更を即座に検出する。
 */
@ExtendWith(MockitoExtension.class)
class ProductControllerContractTest {

    @Mock ProductQueryPort productQuery;
    @Mock ProductCommandPort productCommand;
    @Mock BoAuthService boAuthService;
    @Mock OutboxEventPublisher outboxEventPublisher;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productQuery, productCommand, boAuthService, outboxEventPublisher))
                .build();
    }

    // ── GET /api/item ──────────────────────────────────────────────────────

    @Test
    void getProducts_shouldReturnPagedProductList() throws Exception {
        ProductDto item = new ProductDto();
        item.setId(1L);
        item.setName("AIスピーカー");
        item.setPrice(BigDecimal.valueOf(3980));
        item.setImage("/img/speaker.jpg");
        item.setDescription("AI搭載スピーカー");
        item.setAllocationType(AllocationType.REAL);
        item.setEffectiveStock(100);
        item.setIsPublished(true);
        item.setProductCode("P000001");
        item.setCategoryId(1L);
        item.setCategoryName("未分類");
        ProductListResponse response = new ProductListResponse(List.of(item), 1L, 1, 20);
        when(productQuery.getPublishedProducts(1, 20)).thenReturn(response);

        mockMvc.perform(get("/api/item"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("AIスピーカー"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.limit").value(20));
    }

    // ── GET /api/item/{id} ────────────────────────────────────────────────

    @Test
    void getProduct_shouldReturnProductWithRequiredFields() throws Exception {
        ProductDto product = new ProductDto();
        product.setId(42L);
        product.setName("商品A");
        product.setPrice(BigDecimal.valueOf(1980));
        product.setImage("/img/a.jpg");
        product.setDescription("商品Aの説明");
        product.setAllocationType(AllocationType.FRAME);
        product.setEffectiveStock(50);
        product.setIsPublished(true);
        product.setProductCode("P000042");
        product.setCategoryId(1L);
        product.setCategoryName("未分類");
        when(productQuery.getProduct(42L)).thenReturn(product);

        mockMvc.perform(get("/api/item/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.name").value("商品A"))
                .andExpect(jsonPath("$.data.price").value(1980))
                .andExpect(jsonPath("$.data.isPublished").value(true))
                .andExpect(jsonPath("$.data.allocationType").value("FRAME"))
                .andExpect(jsonPath("$.data.effectiveStock").value(50));
    }

    // ── GET /api/item ページ指定 ──────────────────────────────────────────

    @Test
    void getProducts_withCustomPageParams_shouldForwardToQueryPort() throws Exception {
        ProductListResponse response = new ProductListResponse(List.of(), 0L, 2, 5);
        when(productQuery.getPublishedProducts(2, 5)).thenReturn(response);

        mockMvc.perform(get("/api/item").param("page", "2").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.limit").value(5))
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
