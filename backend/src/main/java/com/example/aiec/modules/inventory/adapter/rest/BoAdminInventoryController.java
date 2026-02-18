package com.example.aiec.modules.inventory.adapter.rest;

import com.example.aiec.modules.shared.dto.ApiResponse;
import com.example.aiec.modules.inventory.adapter.dto.InventoryStatusDto;
import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.inventory.domain.entity.InventoryAdjustment;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ForbiddenException;
import com.example.aiec.modules.inventory.domain.repository.InventoryAdjustmentRepository;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.inventory.application.port.InventoryQueryPort;
import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
import com.example.aiec.modules.shared.event.OperationPerformedEvent;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bo/admin/inventory")
@RequiredArgsConstructor
@Tag(name = "管理（在庫）", description = "在庫管理・調整")
public class BoAdminInventoryController {
    private final InventoryQueryPort inventoryQuery;
    private final InventoryCommandPort inventoryCommand;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final BoAuthService boAuthService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 認可チェック（ADMIN必須）
     */
    private void requireAdmin(BoUser boUser, String requestPath) {
        if (boUser.getPermissionLevel() != PermissionLevel.ADMIN
                && boUser.getPermissionLevel() != PermissionLevel.SUPER_ADMIN) {
            eventPublisher.publishEvent(new OperationPerformedEvent(
                    "AUTHORIZATION_ERROR", boUser.getEmail(), requestPath,
                    "BoUser attempted to access admin resource without permission"));
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
    @Operation(summary = "在庫一覧取得", description = "全商品の在庫状況を取得")
    public ApiResponse<List<InventoryStatusDto>> getAllInventory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/inventory");

        List<InventoryStatusDto> inventories = inventoryQuery.getAllInventoryStatus();
        return ApiResponse.success(inventories);
    }

    /**
     * 在庫調整
     */
    @PostMapping("/adjust")
    @Operation(summary = "在庫調整", description = "指定商品の在庫数を調整")
    public ApiResponse<InventoryAdjustment> adjustStock(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody AdjustStockRequest request) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/inventory/adjust");

        InventoryAdjustment adjustment = inventoryCommand.adjustStock(
                request.getProductId(),
                request.getQuantityDelta(),
                request.getReason(),
                boUser
        );

        // 監査ログ記録イベント発行
        String details = String.format("Adjusted inventory: %s (%d → %d, delta: %+d)",
                adjustment.getProduct().getName(),
                adjustment.getQuantityBefore(),
                adjustment.getQuantityAfter(),
                adjustment.getQuantityDelta());
        eventPublisher.publishEvent(new OperationPerformedEvent(
                "ADMIN_ACTION", boUser.getEmail(), "/api/bo/admin/inventory/adjust", details));

        return ApiResponse.success(adjustment);
    }

    /**
     * 在庫調整履歴取得
     */
    @GetMapping("/adjustments")
    @Operation(summary = "在庫調整履歴取得", description = "在庫調整の履歴を取得")
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
