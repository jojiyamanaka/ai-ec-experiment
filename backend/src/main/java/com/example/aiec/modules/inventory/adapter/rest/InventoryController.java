package com.example.aiec.modules.inventory.adapter.rest;

import com.example.aiec.modules.inventory.application.port.AvailabilityDto;
import com.example.aiec.modules.inventory.adapter.dto.CreateReservationRequest;
import com.example.aiec.modules.inventory.application.port.ReservationDto;
import com.example.aiec.modules.shared.dto.ApiResponse;
import com.example.aiec.modules.inventory.application.port.InventoryQueryPort;
import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 在庫引当コントローラ
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "在庫（内部）", description = "在庫引当の内部API")
public class InventoryController {

    private final InventoryQueryPort inventoryQuery;
    private final InventoryCommandPort inventoryCommand;

    /**
     * 仮引当作成
     * POST /api/inventory/reservations
     */
    @PostMapping("/reservations")
    @Operation(summary = "仮引当作成", description = "指定商品の在庫を仮引当する")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReservationDto> createReservation(
            @RequestHeader("X-Session-Id") String sessionId,
            @Valid @RequestBody CreateReservationRequest request
    ) {
        ReservationDto reservation = inventoryCommand.createReservation(
                sessionId, request.getProductId(), request.getQuantity());
        return ApiResponse.success(reservation);
    }

    /**
     * 仮引当解除
     * DELETE /api/inventory/reservations
     */
    @DeleteMapping("/reservations")
    @Operation(summary = "仮引当解除", description = "仮引当を解除して在庫を戻す")
    public ApiResponse<Void> releaseReservation(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestParam Long productId
    ) {
        inventoryCommand.releaseReservation(sessionId, productId);
        return ApiResponse.success(null);
    }

    /**
     * 本引当（注文確定時に OrderService から内部的に呼ばれる）
     * POST /api/inventory/reservations/commit
     */
    @PostMapping("/reservations/commit")
    @Operation(summary = "本引当", description = "仮引当を本引当に確定する")
    public ApiResponse<Void> commitReservations(
            @RequestHeader("X-Session-Id") String sessionId
    ) {
        // 直接APIとして呼ばれることは想定しないが、エンドポイントとして公開
        // 実際の本引当は OrderService.createOrder 内で行われる
        return ApiResponse.success(null);
    }

    /**
     * 本引当解除（注文キャンセル時）
     * POST /api/inventory/reservations/release
     */
    @PostMapping("/reservations/release")
    @Operation(summary = "本引当解除", description = "本引当を解除して在庫を戻す")
    public ApiResponse<Void> releaseCommittedReservations(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody Map<String, Long> request
    ) {
        Long orderId = request.get("orderId");
        inventoryCommand.releaseCommittedReservations(orderId);
        return ApiResponse.success(null);
    }

    /**
     * 有効在庫確認
     * GET /api/inventory/availability/{productId}
     */
    @GetMapping("/availability/{productId}")
    @Operation(summary = "有効在庫確認", description = "指定商品の有効在庫数を取得")
    public ApiResponse<AvailabilityDto> getAvailability(
            @PathVariable Long productId
    ) {
        AvailabilityDto availability = inventoryQuery.getAvailableStock(productId);
        return ApiResponse.success(availability);
    }

}
