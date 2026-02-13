package com.example.aiec.controller;

import com.example.aiec.dto.ApiResponse;
import com.example.aiec.dto.MemberDetailDto;
import com.example.aiec.dto.UserDto;
import com.example.aiec.entity.BoUser;
import com.example.aiec.entity.PermissionLevel;
import com.example.aiec.entity.User;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ForbiddenException;
import com.example.aiec.exception.ResourceNotFoundException;
import com.example.aiec.repository.OrderRepository;
import com.example.aiec.repository.UserRepository;
import com.example.aiec.service.BoAuthService;
import com.example.aiec.service.OperationHistoryService;
import com.example.aiec.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bo/admin/members")
@RequiredArgsConstructor
public class BoAdminController {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final BoAuthService boAuthService;
    private final OperationHistoryService operationHistoryService;

    private void requireAdmin(BoUser boUser, String requestPath) {
        if (boUser.getPermissionLevel() != PermissionLevel.ADMIN
            && boUser.getPermissionLevel() != PermissionLevel.SUPER_ADMIN) {
            operationHistoryService.logAuthorizationError(boUser, requestPath);
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
        operationHistoryService.logAdminAction(boUser, "/api/bo/admin/members/" + id + "/status", details);

        return ApiResponse.success(UserDto.fromEntity(updated));
    }

    @lombok.Data
    public static class UpdateStatusRequest {
        private Boolean isActive;
    }
}
