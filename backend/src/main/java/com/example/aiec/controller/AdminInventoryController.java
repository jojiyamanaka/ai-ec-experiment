package com.example.aiec.controller;

import com.example.aiec.dto.ApiResponse;
import com.example.aiec.dto.InventoryStatusDto;
import com.example.aiec.entity.InventoryAdjustment;
import com.example.aiec.entity.Role;
import com.example.aiec.entity.User;
import com.example.aiec.exception.ForbiddenException;
import com.example.aiec.repository.InventoryAdjustmentRepository;
import com.example.aiec.service.AuthService;
import com.example.aiec.service.InventoryService;
import com.example.aiec.service.OperationHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {
    private final InventoryService inventoryService;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final AuthService authService;
    private final OperationHistoryService operationHistoryService;

    /**
     * 認可チェック（ADMIN必須）
     */
    private void requireAdmin(User user, String requestPath) {
        if (user.getRole() != Role.ADMIN) {
            operationHistoryService.logAuthorizationError(user, requestPath);
            throw new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません");
        }
    }

    /**
     * Authorization ヘッダーからトークンを抽出
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ForbiddenException("UNAUTHORIZED", "認証が必要です");
        }
        return authHeader.substring(7);
    }

    /**
     * 在庫一覧取得
     */
    @GetMapping
    public ApiResponse<List<InventoryStatusDto>> getAllInventory(
            @RequestHeader("Authorization") String authHeader) {

        String token = extractToken(authHeader);
        User user = authService.verifyToken(token);
        requireAdmin(user, "/api/admin/inventory");

        List<InventoryStatusDto> inventories = inventoryService.getAllInventoryStatus();
        return ApiResponse.success(inventories);
    }

    /**
     * 在庫調整
     */
    @PostMapping("/adjust")
    public ApiResponse<InventoryAdjustment> adjustStock(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AdjustStockRequest request) {

        String token = extractToken(authHeader);
        User user = authService.verifyToken(token);
        requireAdmin(user, "/api/admin/inventory/adjust");

        InventoryAdjustment adjustment = inventoryService.adjustStock(
                request.getProductId(),
                request.getQuantityDelta(),
                request.getReason(),
                user
        );

        // 監査ログ記録
        String details = String.format("Adjusted inventory: %s (%d → %d, delta: %+d)",
                adjustment.getProduct().getName(),
                adjustment.getQuantityBefore(),
                adjustment.getQuantityAfter(),
                adjustment.getQuantityDelta());
        operationHistoryService.logAdminAction(user, "/api/admin/inventory/adjust", details);

        return ApiResponse.success(adjustment);
    }

    /**
     * 在庫調整履歴取得
     */
    @GetMapping("/adjustments")
    public ApiResponse<List<InventoryAdjustment>> getAdjustments(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long productId) {

        String token = extractToken(authHeader);
        User user = authService.verifyToken(token);
        requireAdmin(user, "/api/admin/inventory/adjustments");

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
