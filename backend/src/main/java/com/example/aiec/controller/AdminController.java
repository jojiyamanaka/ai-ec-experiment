package com.example.aiec.controller;

import com.example.aiec.dto.ApiResponse;
import com.example.aiec.dto.MemberDetailDto;
import com.example.aiec.dto.UserDto;
import com.example.aiec.entity.Role;
import com.example.aiec.entity.User;
import com.example.aiec.exception.ForbiddenException;
import com.example.aiec.exception.ResourceNotFoundException;
import com.example.aiec.repository.OrderRepository;
import com.example.aiec.repository.UserRepository;
import com.example.aiec.service.AuthService;
import com.example.aiec.service.OperationHistoryService;
import com.example.aiec.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final AuthService authService;
    private final OperationHistoryService operationHistoryService;

    private void requireAdmin(User user, String requestPath) {
        if (user.getRole() != Role.ADMIN) {
            operationHistoryService.logAuthorizationError(user, requestPath);
            throw new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません");
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ForbiddenException("UNAUTHORIZED", "認証が必要です");
        }
        return authHeader.substring(7);
    }

    /**
     * 会員一覧取得
     */
    @GetMapping
    public ApiResponse<List<UserDto>> getMembers(
            @RequestHeader("Authorization") String authHeader) {

        String token = extractToken(authHeader);
        User user = authService.verifyToken(token);
        requireAdmin(user, "/api/admin/members");

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
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        String token = extractToken(authHeader);
        User user = authService.verifyToken(token);
        requireAdmin(user, "/api/admin/members/" + id);

        User member = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "会員が見つかりません"));

        // 注文サマリ取得
        Long totalOrders = orderRepository.countByUserId(id);
        Long totalAmount = orderRepository.sumTotalPriceByUserId(id).orElse(0L);

        MemberDetailDto dto = new MemberDetailDto();
        dto.setId(member.getId());
        dto.setEmail(member.getEmail());
        dto.setDisplayName(member.getDisplayName());
        dto.setRole(member.getRole());
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
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {

        String token = extractToken(authHeader);
        User user = authService.verifyToken(token);
        requireAdmin(user, "/api/admin/members/" + id + "/status");

        User updated = userService.updateStatus(id, request.getIsActive());

        // 監査ログ記録
        String details = String.format("Updated member status: %s (%s → %s)",
                updated.getEmail(),
                !request.getIsActive() ? "active" : "inactive",
                request.getIsActive() ? "active" : "inactive");
        operationHistoryService.logAdminAction(user, "/api/admin/members/" + id + "/status", details);

        return ApiResponse.success(UserDto.fromEntity(updated));
    }

    /**
     * 会員ロール変更
     */
    @PutMapping("/{id}/role")
    public ApiResponse<UserDto> updateMemberRole(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody UpdateRoleRequest request) {

        String token = extractToken(authHeader);
        User user = authService.verifyToken(token);
        requireAdmin(user, "/api/admin/members/" + id + "/role");

        User updated = userService.updateRole(id, request.getRole());

        // 監査ログ記録
        String details = String.format("Updated member role: %s (%s → %s)",
                updated.getEmail(),
                updated.getRole() == Role.ADMIN ? "CUSTOMER" : "ADMIN",
                request.getRole());
        operationHistoryService.logAdminAction(user, "/api/admin/members/" + id + "/role", details);

        return ApiResponse.success(UserDto.fromEntity(updated));
    }

    @lombok.Data
    public static class UpdateStatusRequest {
        private Boolean isActive;
    }

    @lombok.Data
    public static class UpdateRoleRequest {
        private Role role;
    }
}
