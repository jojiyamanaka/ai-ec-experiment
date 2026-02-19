package com.example.aiec.modules.product.adapter.rest;

import com.example.aiec.modules.shared.dto.ApiResponse;
import com.example.aiec.modules.product.application.port.ProductDto;
import com.example.aiec.modules.product.application.port.ProductListResponse;
import com.example.aiec.modules.product.application.port.UpdateProductRequest;
import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ForbiddenException;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.product.application.port.ProductCommandPort;
import com.example.aiec.modules.product.application.port.ProductQueryPort;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商品コントローラ
 */
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
@Tag(name = "商品", description = "商品の取得・更新")
public class ProductController {

    private final ProductQueryPort productQuery;
    private final ProductCommandPort productCommand;
    private final BoAuthService boAuthService;
    private final OutboxEventPublisher outboxEventPublisher;

    /**
     * 商品一覧取得
     * GET /api/item
     */
    @GetMapping
    @Operation(summary = "商品一覧取得", description = "公開商品の一覧をページネーション付きで取得")
    public ApiResponse<ProductListResponse> getProducts(
            @RequestParam(defaultValue = "1") @Parameter(description = "ページ番号") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "1ページあたりの件数") int limit
    ) {
        ProductListResponse response = productQuery.getPublishedProducts(page, limit);
        return ApiResponse.success(response);
    }

    /**
     * 商品詳細取得
     * GET /api/item/:id
     */
    @GetMapping("/{id}")
    @Operation(summary = "商品詳細取得", description = "指定IDの商品情報を取得")
    public ApiResponse<ProductDto> getProduct(@PathVariable Long id) {
        ProductDto product = productQuery.getProduct(id);
        return ApiResponse.success(product);
    }

    /**
     * 商品更新（管理用）
     * PUT /api/item/:id
     */
    @PutMapping("/{id}")
    @Operation(summary = "商品更新", description = "管理者が商品情報を更新")
    public ApiResponse<ProductDto> updateProduct(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UpdateProductRequest request) {

        // 認証・認可チェック
        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/item/" + id);

        // 管理操作実行
        ProductDto product = productCommand.updateProduct(id, request);

        // 監査ログ記録イベント発行
        outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                "operationType", "ADMIN_ACTION",
                "performedBy", boUser.getEmail(),
                "requestPath", "/api/item/" + id,
                "details", "Updated product: " + product.getName()));

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
    private void requireAdmin(BoUser boUser, String requestPath) {
        if (boUser.getPermissionLevel() != PermissionLevel.ADMIN
                && boUser.getPermissionLevel() != PermissionLevel.SUPER_ADMIN) {
            outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                    "operationType", "AUTHORIZATION_ERROR",
                    "performedBy", boUser.getEmail(),
                    "requestPath", requestPath,
                    "details", "BoUser attempted to access admin resource without permission"));
            throw new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません");
        }
    }

}
