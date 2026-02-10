package com.example.aiec.controller;

import com.example.aiec.dto.*;
import com.example.aiec.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 在庫引当コントローラ
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 仮引当作成
     * POST /api/inventory/reservations
     */
    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReservationDto> createReservation(
            @RequestHeader("X-Session-Id") String sessionId,
            @Valid @RequestBody CreateReservationRequest request
    ) {
        ReservationDto reservation = inventoryService.createReservation(
                sessionId, request.getProductId(), request.getQuantity());
        return ApiResponse.success(reservation);
    }

    /**
     * 仮引当解除
     * DELETE /api/inventory/reservations
     */
    @DeleteMapping("/reservations")
    public ApiResponse<Void> releaseReservation(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestParam Long productId
    ) {
        inventoryService.releaseReservation(sessionId, productId);
        return ApiResponse.success(null);
    }

    /**
     * 本引当（注文確定時に OrderService から内部的に呼ばれる）
     * POST /api/inventory/reservations/commit
     */
    @PostMapping("/reservations/commit")
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
    public ApiResponse<Void> releaseCommittedReservations(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody Map<String, Long> request
    ) {
        Long orderId = request.get("orderId");
        inventoryService.releaseCommittedReservations(orderId);
        return ApiResponse.success(null);
    }

    /**
     * 有効在庫確認
     * GET /api/inventory/availability/{productId}
     */
    @GetMapping("/availability/{productId}")
    public ApiResponse<AvailabilityDto> getAvailability(
            @PathVariable Long productId
    ) {
        AvailabilityDto availability = inventoryService.getAvailableStock(productId);
        return ApiResponse.success(availability);
    }

}
