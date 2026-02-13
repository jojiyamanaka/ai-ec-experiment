package com.example.aiec.controller;

import com.example.aiec.dto.ApiResponse;
import com.example.aiec.dto.InventoryStatusDto;
import com.example.aiec.entity.BoUser;
import com.example.aiec.entity.InventoryAdjustment;
import com.example.aiec.entity.PermissionLevel;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ForbiddenException;
import com.example.aiec.repository.InventoryAdjustmentRepository;
import com.example.aiec.service.BoAuthService;
import com.example.aiec.service.InventoryService;
import com.example.aiec.service.OperationHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bo/admin/inventory")
@RequiredArgsConstructor
public class BoAdminInventoryController {
    private final InventoryService inventoryService;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final BoAuthService boAuthService;
    private final OperationHistoryService operationHistoryService;

    /**
     * 認可チェック（ADMIN必須）
     */
    private void requireAdmin(BoUser boUser, String requestPath) {
        if (boUser.getPermissionLevel() != PermissionLevel.ADMIN
                && boUser.getPermissionLevel() != PermissionLevel.SUPER_ADMIN) {
            operationHistoryService.logAuthorizationError(boUser, requestPath);
            throw new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません");
        }
    }

    /**
     * Authorization ヘッダーからトークンを抽出
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("UNAUTHORIZED", "認証が必要です");
        }
        return authHeader.substring(7);
    }

    /**
     * 在庫一覧取得
     */
    @GetMapping
    public ApiResponse<List<InventoryStatusDto>> getAllInventory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/inventory");

        List<InventoryStatusDto> inventories = inventoryService.getAllInventoryStatus();
        return ApiResponse.success(inventories);
    }

    /**
     * 在庫調整
     */
    @PostMapping("/adjust")
    public ApiResponse<InventoryAdjustment> adjustStock(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody AdjustStockRequest request) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/inventory/adjust");

        InventoryAdjustment adjustment = inventoryService.adjustStock(
                request.getProductId(),
                request.getQuantityDelta(),
                request.getReason(),
                boUser
        );

        // 監査ログ記録
        String details = String.format("Adjusted inventory: %s (%d → %d, delta: %+d)",
                adjustment.getProduct().getName(),
                adjustment.getQuantityBefore(),
                adjustment.getQuantityAfter(),
                adjustment.getQuantityDelta());
        operationHistoryService.logAdminAction(boUser, "/api/bo/admin/inventory/adjust", details);

        return ApiResponse.success(adjustment);
    }

    /**
     * 在庫調整履歴取得
     */
    @GetMapping("/adjustments")
    public ApiResponse<List<InventoryAdjustment>> getAdjustments(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) Long productId) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/inventory/adjustments");

        List<InventoryAdjustment> adjustments = productId != null
                ? adjustmentRepository.findByProductIdOrderByAdjustedAtDesc(productId)
                : adjustmentRepository.findAllByOrderByAdjustedAtDesc();

        return ApiResponse.success(adjustments);
    }

    /**
     * 在庫調整リクエストDTO
     */
    @lombok.Data
    public static class AdjustStockRequest {
        private Long productId;
        private Integer quantityDelta;
        private String reason;
    }
}
