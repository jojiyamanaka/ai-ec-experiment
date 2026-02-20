package com.example.aiec.modules.product.adapter.rest;

import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.product.application.port.ProductCommandPort;
import com.example.aiec.modules.product.application.port.ProductDto;
import com.example.aiec.modules.product.application.port.ProductListResponse;
import com.example.aiec.modules.product.application.port.ProductQueryPort;
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
        ProductDto item = new ProductDto(1L, "AIスピーカー", BigDecimal.valueOf(3980),
                "/img/speaker.jpg", "AI搭載スピーカー", 100, true,
                "P000001", 1L, "未分類", null, null, null, null);
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
        ProductDto product = new ProductDto(42L, "商品A", BigDecimal.valueOf(1980),
                "/img/a.jpg", "商品Aの説明", 50, true,
                "P000042", 1L, "未分類", null, null, null, null);
        when(productQuery.getProduct(42L)).thenReturn(product);

        mockMvc.perform(get("/api/item/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.name").value("商品A"))
                .andExpect(jsonPath("$.data.price").value(1980))
                .andExpect(jsonPath("$.data.isPublished").value(true))
                .andExpect(jsonPath("$.data.stock").value(50));
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
