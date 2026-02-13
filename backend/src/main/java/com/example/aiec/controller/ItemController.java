package com.example.aiec.controller;

import com.example.aiec.dto.ApiResponse;
import com.example.aiec.dto.ProductDto;
import com.example.aiec.dto.ProductListResponse;
import com.example.aiec.dto.UpdateProductRequest;
import com.example.aiec.entity.Role;
import com.example.aiec.entity.User;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ForbiddenException;
import com.example.aiec.service.AuthService;
import com.example.aiec.service.OperationHistoryService;
import com.example.aiec.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品コントローラ
 */
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class ItemController {

    private final ProductService productService;
    private final AuthService authService;
    private final OperationHistoryService operationHistoryService;

    /**
     * 商品一覧取得
     * GET /api/item
     */
    @GetMapping
    public ApiResponse<ProductListResponse> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        ProductListResponse response = productService.getPublishedProducts(page, limit);
        return ApiResponse.success(response);
    }

    /**
     * 商品詳細取得
     * GET /api/item/:id
     */
    @GetMapping("/{id}")
    public ApiResponse<ProductDto> getProduct(@PathVariable Long id) {
        ProductDto product = productService.getProductById(id);
        return ApiResponse.success(product);
    }

    /**
     * 商品更新（管理用）
     * PUT /api/item/:id
     */
    @PutMapping("/{id}")
    public ApiResponse<ProductDto> updateProduct(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProductRequest request) {

        // 認証・認可チェック
        String token = extractToken(authHeader);
        User user = authService.verifyToken(token);
        requireAdmin(user, "/api/item/" + id);

        // 管理操作実行
        ProductDto product = productService.updateProduct(id, request);

        // 操作履歴記録
        operationHistoryService.logAdminAction(user, "/api/item/" + id,
            "Updated product: " + product.getName());

        return ApiResponse.success(product);
    }

    /**
     * Authorizationヘッダーからトークンを抽出
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("UNAUTHORIZED", "認証が必要です");
        }
        return authHeader.substring(7);
    }

    /**
     * 管理者権限チェック
     */
    private void requireAdmin(User user, String requestPath) {
        if (user.getRole() != Role.ADMIN) {
            operationHistoryService.logAuthorizationError(user, requestPath);
            throw new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません");
        }
    }

}
