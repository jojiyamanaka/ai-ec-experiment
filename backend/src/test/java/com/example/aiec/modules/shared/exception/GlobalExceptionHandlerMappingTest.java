package com.example.aiec.modules.shared.exception;

import com.example.aiec.modules.inventory.application.port.StockShortageDetail;
import com.example.aiec.modules.purchase.application.port.UnavailableProductDetail;
import com.example.aiec.modules.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler の例外→HTTPステータス・レスポンス形式マッピングテスト。
 *
 * standaloneSetup を使い Spring Security・JPA 等への依存なしで動作する。
 * 例外ハンドラのマッピングが壊れた場合（例: エラーコード追加漏れ）に即座に検出する。
 */
class GlobalExceptionHandlerMappingTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── 401 Unauthorized ────────────────────────────────────────────────────

    @Test
    void businessExceptionUnauthorized_shouldReturn401() throws Exception {
        mockMvc.perform(get("/test/business-unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void missingAuthorizationHeader_shouldReturn401() throws Exception {
        mockMvc.perform(get("/test/missing-auth-header"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    // ── 400 Bad Request ──────────────────────────────────────────────────────

    @Test
    void methodArgumentNotValid_shouldReturn400WithFieldMessage() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("name is required"));
    }

    @Test
    void itemNotAvailable_shouldReturn400WithDetails() throws Exception {
        mockMvc.perform(get("/test/item-not-available"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ITEM_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.error.details[0].productId").value(99));
    }

    // ── 403 Forbidden ────────────────────────────────────────────────────────

    @Test
    void forbiddenException_shouldReturn403() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    // ── 404 Not Found ────────────────────────────────────────────────────────

    @Test
    void resourceNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // ── 409 Conflict ─────────────────────────────────────────────────────────

    @Test
    void conflictException_shouldReturn409() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE"));
    }

    @Test
    void insufficientStock_shouldReturn409WithDetails() throws Exception {
        mockMvc.perform(get("/test/insufficient-stock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.error.details[0].productId").value(1))
                .andExpect(jsonPath("$.error.details[0].availableStock").value(3));
    }

    // ── Controller ───────────────────────────────────────────────────────────

    @RestController
    static class TestController {

        @GetMapping("/test/business-unauthorized")
        public ApiResponse<Void> throwBusinessUnauthorized() {
            throw new BusinessException("UNAUTHORIZED", "認証が必要です");
        }

        @GetMapping("/test/missing-auth-header")
        public ApiResponse<Void> requireAuthorizationHeader(
                @RequestHeader("Authorization") String authorization
        ) {
            return ApiResponse.success(null);
        }

        @PostMapping("/test/validation")
        public ApiResponse<Void> validateRequest(@Valid @RequestBody TestRequest request) {
            return ApiResponse.success(null);
        }

        @GetMapping("/test/item-not-available")
        public ApiResponse<Void> throwItemNotAvailable() {
            throw new ItemNotAvailableException(
                    "ITEM_NOT_AVAILABLE", "購入できない商品があります",
                    List.of(new UnavailableProductDetail(99L, "廃番商品")));
        }

        @GetMapping("/test/forbidden")
        public ApiResponse<Void> throwForbidden() {
            throw new ForbiddenException("FORBIDDEN", "権限がありません");
        }

        @GetMapping("/test/not-found")
        public ApiResponse<Void> throwNotFound() {
            throw new ResourceNotFoundException("NOT_FOUND", "リソースが見つかりません");
        }

        @GetMapping("/test/conflict")
        public ApiResponse<Void> throwConflict() {
            throw new ConflictException("DUPLICATE", "既に存在します");
        }

        @GetMapping("/test/insufficient-stock")
        public ApiResponse<Void> throwInsufficientStock() {
            throw new InsufficientStockException(
                    "OUT_OF_STOCK", "在庫が不足しています",
                    List.of(new StockShortageDetail(1L, "商品A", 5, 3)));
        }
    }

    static class TestRequest {

        @NotBlank(message = "name is required")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
