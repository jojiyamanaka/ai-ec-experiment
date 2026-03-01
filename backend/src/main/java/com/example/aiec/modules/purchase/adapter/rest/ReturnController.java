package com.example.aiec.modules.purchase.adapter.rest;

import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.customer.domain.service.AuthService;
import com.example.aiec.modules.purchase.application.port.CreateReturnRequest;
import com.example.aiec.modules.purchase.application.port.RejectReturnRequest;
import com.example.aiec.modules.purchase.application.port.ReturnCommandPort;
import com.example.aiec.modules.purchase.application.port.ReturnListResponse;
import com.example.aiec.modules.purchase.application.port.ReturnQueryPort;
import com.example.aiec.modules.purchase.application.port.ReturnShipmentDto;
import com.example.aiec.modules.purchase.shipment.entity.Shipment;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.shared.dto.ApiResponse;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ForbiddenException;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "返品", description = "返品申請・承認・取得")
public class ReturnController {

    private final ReturnCommandPort returnCommand;
    private final ReturnQueryPort returnQuery;
    private final AuthService authService;
    private final BoAuthService boAuthService;
    private final OutboxEventPublisher outboxEventPublisher;

    @PostMapping("/api/order/{orderId}/return")
    @Operation(summary = "返品申請", description = "顧客が返品を申請")
    public ApiResponse<ReturnShipmentDto> createReturn(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateReturnRequest request
    ) {
        User user = authService.verifyToken(extractToken(authHeader));
        ReturnShipmentDto response = returnCommand.createReturn(orderId, user.getId(), request);
        return ApiResponse.success(response);
    }

    @GetMapping("/api/order/{orderId}/return")
    @Operation(summary = "返品取得", description = "返品情報を取得")
    public ApiResponse<ReturnShipmentDto> getReturnByOrderId(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        Long userId = null;

        try {
            User user = authService.verifyToken(token);
            userId = user.getId();
        } catch (BusinessException ex) {
            BoUser boUser = boAuthService.verifyToken(token);
            requireAdmin(boUser, "/api/order/" + orderId + "/return");
        }

        ReturnShipmentDto response = returnQuery.getReturnByOrderId(orderId, userId);
        return ApiResponse.success(response);
    }

    @PostMapping("/api/order/{orderId}/return/approve")
    @Operation(summary = "返品承認", description = "管理者が返品を承認")
    public ApiResponse<ReturnShipmentDto> approveReturn(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader
    ) {
        BoUser boUser = boAuthService.verifyToken(extractToken(authHeader));
        requireAdmin(boUser, "/api/order/" + orderId + "/return/approve");
        return ApiResponse.success(returnCommand.approveReturn(orderId));
    }

    @PostMapping("/api/order/{orderId}/return/reject")
    @Operation(summary = "返品拒否", description = "管理者が返品を拒否")
    public ApiResponse<ReturnShipmentDto> rejectReturn(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody RejectReturnRequest request
    ) {
        BoUser boUser = boAuthService.verifyToken(extractToken(authHeader));
        requireAdmin(boUser, "/api/order/" + orderId + "/return/reject");
        return ApiResponse.success(returnCommand.rejectReturn(orderId, request));
    }

    @PostMapping("/api/order/{orderId}/return/confirm")
    @Operation(summary = "返品確定", description = "管理者が返品を確定")
    public ApiResponse<ReturnShipmentDto> confirmReturn(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader
    ) {
        BoUser boUser = boAuthService.verifyToken(extractToken(authHeader));
        requireAdmin(boUser, "/api/order/" + orderId + "/return/confirm");
        return ApiResponse.success(returnCommand.confirmReturn(orderId));
    }

    @GetMapping("/api/return")
    @Operation(summary = "返品一覧", description = "管理者が返品一覧を取得")
    public ApiResponse<ReturnListResponse> getAllReturns(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        BoUser boUser = boAuthService.verifyToken(extractToken(authHeader));
        requireAdmin(boUser, "/api/return");
        Shipment.ShipmentStatus shipmentStatus = parseStatus(status);
        return ApiResponse.success(returnQuery.getAllReturns(shipmentStatus, page, limit));
    }

    private Shipment.ShipmentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return Shipment.ShipmentStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("INVALID_REQUEST", "リクエストパラメータが不正です");
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("UNAUTHORIZED", "認証が必要です");
        }
        return authHeader.substring(7);
    }

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
