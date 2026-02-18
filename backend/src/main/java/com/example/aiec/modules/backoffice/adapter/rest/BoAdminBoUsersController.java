package com.example.aiec.modules.backoffice.adapter.rest;

import com.example.aiec.modules.shared.dto.ApiResponse;
import com.example.aiec.modules.backoffice.adapter.dto.BoUserDto;
import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.shared.domain.model.PermissionLevel;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ForbiddenException;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.backoffice.domain.service.BoUserService;
import com.example.aiec.modules.shared.event.OperationPerformedEvent;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bo/admin/bo-users")
@RequiredArgsConstructor
@Tag(name = "管理（BoUser）", description = "BoUser管理")
public class BoAdminBoUsersController {

    private final BoUserService boUserService;
    private final BoAuthService boAuthService;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping
    @Operation(summary = "BoUser一覧取得", description = "全管理者ユーザーの一覧を取得")
    public ApiResponse<List<BoUserDto>> getBoUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = extractToken(authHeader);
        BoUser actor = boAuthService.verifyToken(token);
        requireAdmin(actor, "/api/bo/admin/bo-users");

        List<BoUserDto> boUsers = boUserService.findAll().stream()
                .map(BoUserDto::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(boUsers);
    }

    @PostMapping
    @Operation(summary = "BoUser作成", description = "新規管理者ユーザーを作成")
    public ApiResponse<BoUserDto> createBoUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody CreateBoUserRequest request) {

        String token = extractToken(authHeader);
        BoUser actor = boAuthService.verifyToken(token);
        requireAdmin(actor, "/api/bo/admin/bo-users");

        String displayName = request.getDisplayName() != null && !request.getDisplayName().isBlank()
                ? request.getDisplayName()
                : (request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername()
                : request.getEmail());

        BoUser created = boUserService.createBoUser(
                request.getEmail(),
                displayName,
                request.getPassword(),
                PermissionLevel.ADMIN
        );

        eventPublisher.publishEvent(new OperationPerformedEvent(
                "BO_USER_CREATE", actor.getEmail(), "/api/bo/admin/bo-users",
                "Created BoUser: " + created.getEmail()));

        return ApiResponse.success(BoUserDto.fromEntity(created));
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
            eventPublisher.publishEvent(new OperationPerformedEvent(
                    "AUTHORIZATION_ERROR", boUser.getEmail(), requestPath,
                    "BoUser attempted to access admin resource without permission"));
            throw new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません");
        }
    }

    @Data
    public static class CreateBoUserRequest {
        private String username;
        private String displayName;
        private String email;
        private String password;
    }
}
