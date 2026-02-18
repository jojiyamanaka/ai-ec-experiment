package com.example.aiec.modules.backoffice.adapter.rest;

import com.example.aiec.modules.backoffice.adapter.dto.BoAuthResponse;
import com.example.aiec.modules.backoffice.adapter.dto.BoLoginRequest;
import com.example.aiec.modules.shared.dto.ApiResponse;
import com.example.aiec.modules.backoffice.adapter.dto.BoUserDto;
import com.example.aiec.modules.backoffice.domain.entity.BoUser;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
import com.example.aiec.modules.backoffice.domain.service.BoAuthService;
import com.example.aiec.modules.backoffice.domain.service.BoUserService;
import com.example.aiec.modules.shared.event.OperationPerformedEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/bo-auth")
@RequiredArgsConstructor
@Tag(name = "管理者認証", description = "管理者のログイン・ログアウト")
public class BoAuthController {

    private final BoUserService boUserService;
    private final BoAuthService boAuthService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * BoUser ログイン
     */
    @PostMapping("/login")
    @Operation(summary = "管理者ログイン", description = "BoUserのメールアドレスとパスワードで認証")
    public ApiResponse<BoAuthResponse> login(@Valid @RequestBody BoLoginRequest request) {
        try {
            // 1. BoUser 検証
            BoUser boUser = boUserService.findByEmail(request.getEmail());
            if (!boUserService.verifyPassword(boUser, request.getPassword())) {
                eventPublisher.publishEvent(new OperationPerformedEvent(
                        "LOGIN_FAILURE", request.getEmail(), "/api/bo-auth/login", "Login attempt failed"));
                throw new BusinessException("INVALID_CREDENTIALS", "メールアドレスまたはパスワードが正しくありません");
            }

            // 2. 有効チェック
            if (!boUser.getIsActive()) {
                throw new BusinessException("BO_USER_INACTIVE", "このアカウントは無効化されています");
            }

            // 3. トークン発行
            BoAuthService.TokenPair tokenPair = boAuthService.createToken(boUser);

            // 4. 最終ログイン日時を更新
            boUser.setLastLoginAt(Instant.now());

            // 5. ログイン成功を記録
            eventPublisher.publishEvent(new OperationPerformedEvent(
                    "LOGIN_SUCCESS", boUser.getEmail(), "/api/bo-auth/login",
                    "BoUser login successful: " + boUser.getEmail()));

            // 6. レスポンス
            BoAuthResponse response = new BoAuthResponse(
                    BoUserDto.fromEntity(boUser),
                    tokenPair.getRawToken(),
                    tokenPair.getAuthToken().getExpiresAt()
            );
            return ApiResponse.success(response);

        } catch (ResourceNotFoundException ex) {
            if ("BO_USER_NOT_FOUND".equals(ex.getErrorCode())) {
                eventPublisher.publishEvent(new OperationPerformedEvent(
                        "LOGIN_FAILURE", request.getEmail(), "/api/bo-auth/login", "Login attempt failed"));
                throw new BusinessException("INVALID_CREDENTIALS", "メールアドレスまたはパスワードが正しくありません");
            }
            throw ex;
        }
    }

    /**
     * BoUser ログアウト
     */
    @PostMapping("/logout")
    @Operation(summary = "管理者ログアウト", description = "管理者の認証トークンを失効させる")
    public ApiResponse<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        boAuthService.revokeToken(token);
        return ApiResponse.success(Map.of("message", "ログアウトしました"));
    }

    /**
     * BoUser 情報取得
     */
    @GetMapping("/me")
    @Operation(summary = "管理者情報取得", description = "認証済み管理者の情報を取得")
    public ApiResponse<BoUserDto> getCurrentBoUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        return ApiResponse.success(BoUserDto.fromEntity(boUser));
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
}
