package com.example.aiec.modules.product.adapter.rest;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.inventory.application.port.AdminItemInventoryDto;
import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
import com.example.aiec.modules.inventory.application.port.InventoryQueryPort;
import com.example.aiec.modules.inventory.application.port.UpdateItemInventoryRequest;
import com.example.aiec.modules.product.application.port.CreateProductCategoryRequest;
import com.example.aiec.modules.product.application.port.CreateProductRequest;
import com.example.aiec.modules.product.application.port.AdminProductSearchParams;
import com.example.aiec.modules.product.application.port.ProductCategoryDto;
import com.example.aiec.modules.product.application.port.ProductCommandPort;
import com.example.aiec.modules.product.application.port.ProductDto;
import com.example.aiec.modules.product.application.port.ProductListResponse;
import com.example.aiec.modules.product.application.port.ProductQueryPort;
import com.example.aiec.modules.product.application.port.UpdateProductCategoryRequest;
import com.example.aiec.modules.product.application.port.UpdateProductRequest;
import com.example.aiec.modules.product.domain.entity.AllocationType;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.shared.dto.ApiResponse;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ForbiddenException;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "管理（商品）", description = "管理者向け商品・カテゴリ管理")
public class BoAdminProductController {

    private final ProductQueryPort productQuery;
    private final ProductCommandPort productCommand;
    private final BoAuthService boAuthService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final InventoryQueryPort inventoryQuery;
    private final InventoryCommandPort inventoryCommand;

    @GetMapping({"/api/bo/admin/items", "/api/admin/items"})
    @Operation(summary = "管理向け商品一覧取得", description = "公開状態に関係なく商品一覧を取得")
    public ApiResponse<ProductListResponse> getAdminProducts(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "1") @Parameter(description = "ページ番号") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "1ページあたりの件数") int limit,
            @RequestParam(required = false) @Parameter(description = "商品名・品番の部分一致") String keyword,
            @RequestParam(required = false) @Parameter(description = "カテゴリID完全一致") Long categoryId,
            @RequestParam(required = false) @Parameter(description = "公開状態フィルタ") Boolean isPublished,
            @RequestParam(required = false) @Parameter(description = "現在販売期間内のみ") Boolean inSalePeriod,
            @RequestParam(required = false) @Parameter(description = "引当区分フィルタ") String allocationType,
            @RequestParam(required = false) @Parameter(description = "有効在庫閾値以下") Integer stockThreshold,
            @RequestParam(required = false) @Parameter(description = "有効在庫0のみ") Boolean zeroStockOnly) {
        BoUser boUser = verifyAdmin(authHeader, "/api/bo/admin/items");
        if (stockThreshold != null && stockThreshold < 0) {
            throw new BusinessException("INVALID_REQUEST", "リクエストパラメータが不正です");
        }
        AllocationType parsedAllocationType = null;
        if (allocationType != null && !allocationType.isBlank()) {
            try {
                parsedAllocationType = AllocationType.valueOf(allocationType);
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("INVALID_REQUEST", "リクエストパラメータが不正です");
            }
        }
        AdminProductSearchParams searchParams = new AdminProductSearchParams(
                keyword,
                categoryId,
                isPublished,
                inSalePeriod,
                parsedAllocationType,
                stockThreshold,
                zeroStockOnly
        );
        ProductListResponse response = productQuery.getAdminProducts(searchParams, page, limit);
        publishAudit("ADMIN_ACTION", boUser.getEmail(), "/api/bo/admin/items", "Fetched admin product list");
        return ApiResponse.success(response);
    }

    @GetMapping({"/api/bo/admin/items/{id}", "/api/admin/items/{id}"})
    @Operation(summary = "管理向け商品詳細取得", description = "公開状態に関係なく商品詳細を取得")
    public ApiResponse<ProductDto> getAdminProduct(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        BoUser boUser = verifyAdmin(authHeader, "/api/bo/admin/items/" + id);
        ProductDto response = productQuery.getAdminProduct(id);
        publishAudit("ADMIN_ACTION", boUser.getEmail(), "/api/bo/admin/items/" + id, "Fetched admin product detail");
        return ApiResponse.success(response);
    }

    @PostMapping({"/api/bo/admin/items", "/api/admin/items"})
    @Operation(summary = "管理向け商品新規登録", description = "管理者が商品を新規登録")
    public ApiResponse<ProductDto> createProduct(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CreateProductRequest request) {
        BoUser boUser = verifyAdmin(authHeader, "/api/bo/admin/items");
        ProductDto response = productCommand.createProduct(request);
        publishAudit("ADMIN_ACTION", boUser.getEmail(), "/api/bo/admin/items", "Created product: " + response.getName());
        return ApiResponse.success(response);
    }

    @PutMapping({"/api/bo/admin/items/{id}", "/api/admin/items/{id}"})
    @Operation(summary = "管理向け商品更新", description = "管理者が商品情報を更新")
    public ApiResponse<ProductDto> updateProduct(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        BoUser boUser = verifyAdmin(authHeader, "/api/bo/admin/items/" + id);
        ProductDto response = productCommand.updateProduct(id, request);
        publishAudit("ADMIN_ACTION", boUser.getEmail(), "/api/bo/admin/items/" + id, "Updated product: " + response.getName());
        return ApiResponse.success(response);
    }

    @GetMapping({"/api/bo/admin/items/{id}/inventory", "/api/admin/items/{id}/inventory"})
    @Operation(summary = "管理向け在庫タブ取得", description = "商品の在庫タブ表示情報を取得")
    public ApiResponse<AdminItemInventoryDto> getItemInventory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        BoUser boUser = verifyAdmin(authHeader, "/api/bo/admin/items/" + id + "/inventory");
        AdminItemInventoryDto response = inventoryQuery.getAdminItemInventory(id);
        publishAudit("ADMIN_ACTION", boUser.getEmail(), "/api/bo/admin/items/" + id + "/inventory", "Fetched inventory tab data");
        return ApiResponse.success(response);
    }

    @PutMapping({"/api/bo/admin/items/{id}/inventory", "/api/admin/items/{id}/inventory"})
    @Operation(summary = "管理向け在庫タブ更新", description = "商品の在庫タブ情報を更新")
    public ApiResponse<AdminItemInventoryDto> updateItemInventory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody UpdateItemInventoryRequest request) {
        BoUser boUser = verifyAdmin(authHeader, "/api/bo/admin/items/" + id + "/inventory");
        AdminItemInventoryDto response = inventoryCommand.updateAdminItemInventory(id, request, boUser);
        publishAudit("ADMIN_ACTION", boUser.getEmail(), "/api/bo/admin/items/" + id + "/inventory", "Updated inventory tab data");
        return ApiResponse.success(response);
    }

    @GetMapping({"/api/bo/admin/item-categories", "/api/admin/item-categories"})
    @Operation(summary = "管理向けカテゴリ一覧取得", description = "カテゴリ一覧を表示順で取得")
    public ApiResponse<List<ProductCategoryDto>> getCategories(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        BoUser boUser = verifyAdmin(authHeader, "/api/bo/admin/item-categories");
        List<ProductCategoryDto> response = productCommand.getCategories();
        publishAudit("ADMIN_ACTION", boUser.getEmail(), "/api/bo/admin/item-categories", "Fetched product categories");
        return ApiResponse.success(response);
    }

    @PostMapping({"/api/bo/admin/item-categories", "/api/admin/item-categories"})
    @Operation(summary = "管理向けカテゴリ新規登録", description = "カテゴリを新規登録")
    public ApiResponse<ProductCategoryDto> createCategory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CreateProductCategoryRequest request) {
        BoUser boUser = verifyAdmin(authHeader, "/api/bo/admin/item-categories");
        ProductCategoryDto response = productCommand.createCategory(request);
        publishAudit("ADMIN_ACTION", boUser.getEmail(), "/api/bo/admin/item-categories", "Created category: " + response.getName());
        return ApiResponse.success(response);
    }

    @PutMapping({"/api/bo/admin/item-categories/{id}", "/api/admin/item-categories/{id}"})
    @Operation(summary = "管理向けカテゴリ更新", description = "カテゴリ名称・表示順・公開状態を更新")
    public ApiResponse<ProductCategoryDto> updateCategory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductCategoryRequest request) {
        BoUser boUser = verifyAdmin(authHeader, "/api/bo/admin/item-categories/" + id);
        ProductCategoryDto response = productCommand.updateCategory(id, request);
        publishAudit("ADMIN_ACTION", boUser.getEmail(), "/api/bo/admin/item-categories/" + id, "Updated category: " + response.getName());
        return ApiResponse.success(response);
    }

    private BoUser verifyAdmin(String authHeader, String requestPath) {
        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        if (boUser.getPermissionLevel() != PermissionLevel.ADMIN
                && boUser.getPermissionLevel() != PermissionLevel.SUPER_ADMIN) {
            publishAudit("AUTHORIZATION_ERROR", boUser.getEmail(), requestPath,
                    "BoUser attempted to access admin resource without permission");
            throw new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません");
        }
        return boUser;
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("UNAUTHORIZED", "認証が必要です");
        }
        return authHeader.substring(7);
    }

    private void publishAudit(String operationType, String performedBy, String requestPath, String details) {
        outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                "operationType", operationType,
                "performedBy", performedBy,
                "requestPath", requestPath,
                "details", details));
    }
}
