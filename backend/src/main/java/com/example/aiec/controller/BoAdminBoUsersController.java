package com.example.aiec.controller;

import com.example.aiec.dto.ApiResponse;
import com.example.aiec.dto.BoUserDto;
import com.example.aiec.entity.BoUser;
import com.example.aiec.entity.PermissionLevel;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ForbiddenException;
import com.example.aiec.service.BoAuthService;
import com.example.aiec.service.BoUserService;
import com.example.aiec.service.OperationHistoryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bo/admin/bo-users")
@RequiredArgsConstructor
public class BoAdminBoUsersController {

    private final BoUserService boUserService;
    private final BoAuthService boAuthService;
    private final OperationHistoryService operationHistoryService;

    @GetMapping
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

        operationHistoryService.logAdminAction(
                actor,
                "/api/bo/admin/bo-users",
                "Created BoUser: " + created.getEmail()
        );

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
            operationHistoryService.logAuthorizationError(boUser, requestPath);
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
