package com.example.aiec.modules.backoffice.adapter.rest;

import com.example.aiec.modules.shared.dto.ApiResponse;
import com.example.aiec.modules.backoffice.adapter.dto.MemberDetailDto;
import com.example.aiec.modules.customer.adapter.dto.UserDto;
import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ForbiddenException;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import com.example.aiec.modules.purchase.order.repository.OrderRepository;
import com.example.aiec.modules.customer.domain.repository.UserRepository;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.customer.domain.service.UserService;
import com.example.aiec.modules.shared.event.OperationPerformedEvent;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bo/admin/members")
@RequiredArgsConstructor
@Tag(name = "管理（会員）", description = "会員管理")
public class BoAdminController {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final BoAuthService boAuthService;
    private final ApplicationEventPublisher eventPublisher;

    private void requireAdmin(BoUser boUser, String requestPath) {
        if (boUser.getPermissionLevel() != PermissionLevel.ADMIN
            && boUser.getPermissionLevel() != PermissionLevel.SUPER_ADMIN) {
            eventPublisher.publishEvent(new OperationPerformedEvent(
                    "AUTHORIZATION_ERROR", boUser.getEmail(), requestPath,
                    "BoUser attempted to access admin resource without permission"));
            throw new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません");
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("UNAUTHORIZED", "認証が必要です");
        }
        return authHeader.substring(7);
    }

    /**
     * 会員一覧取得
     */
    @GetMapping
    @Operation(summary = "会員一覧取得", description = "全会員の一覧を取得")
    public ApiResponse<List<UserDto>> getMembers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/members");

        List<UserDto> members = userRepository.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(members);
    }

    /**
     * 会員詳細取得
     */
    @GetMapping("/{id}")
    @Operation(summary = "会員詳細取得", description = "指定IDの会員詳細と注文サマリを取得")
    public ApiResponse<MemberDetailDto> getMemberById(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/members/" + id);

        User member = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "会員が見つかりません"));

        Long totalOrders = orderRepository.countByUserId(id);
        BigDecimal totalAmount = orderRepository.sumTotalPriceByUserId(id).orElse(BigDecimal.ZERO);

        MemberDetailDto dto = new MemberDetailDto();
        dto.setId(member.getId());
        dto.setEmail(member.getEmail());
        dto.setDisplayName(member.getDisplayName());
        dto.setIsActive(member.getIsActive());
        dto.setCreatedAt(member.getCreatedAt());
        dto.setUpdatedAt(member.getUpdatedAt());

        MemberDetailDto.OrderSummary orderSummary = new MemberDetailDto.OrderSummary();
        orderSummary.setTotalOrders(totalOrders);
        orderSummary.setTotalAmount(totalAmount);
        dto.setOrderSummary(orderSummary);

        return ApiResponse.success(dto);
    }

    /**
     * 会員状態変更
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "会員状態変更", description = "会員の有効/無効状態を変更")
    public ApiResponse<UserDto> updateMemberStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/members/" + id + "/status");

        User updated = userService.updateStatus(id, request.getIsActive());

        String details = String.format("Updated member status: %s (%s → %s)",
                updated.getEmail(),
                !request.getIsActive() ? "active" : "inactive",
                request.getIsActive() ? "active" : "inactive");
        eventPublisher.publishEvent(new OperationPerformedEvent(
                "ADMIN_ACTION", boUser.getEmail(), "/api/bo/admin/members/" + id + "/status", details));

        return ApiResponse.success(UserDto.fromEntity(updated));
    }

    @lombok.Data
    public static class UpdateStatusRequest {
        private Boolean isActive;
    }
}
