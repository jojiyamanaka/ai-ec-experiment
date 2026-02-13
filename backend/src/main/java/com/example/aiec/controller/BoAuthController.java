package com.example.aiec.controller;

import com.example.aiec.dto.*;
import com.example.aiec.entity.BoUser;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ResourceNotFoundException;
import com.example.aiec.service.BoAuthService;
import com.example.aiec.service.BoUserService;
import com.example.aiec.service.OperationHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/bo-auth")
@RequiredArgsConstructor
public class BoAuthController {

    private final BoUserService boUserService;
    private final BoAuthService boAuthService;
    private final OperationHistoryService operationHistoryService;

    /**
     * BoUser ログイン
     */
    @PostMapping("/login")
    public ApiResponse<BoAuthResponse> login(@Valid @RequestBody BoLoginRequest request) {
        try {
            // 1. BoUser 検証
            BoUser boUser = boUserService.findByEmail(request.getEmail());
            if (!boUserService.verifyPassword(boUser, request.getPassword())) {
                operationHistoryService.logLoginFailure(request.getEmail());
                throw new BusinessException("INVALID_CREDENTIALS", "メールアドレスまたはパスワードが正しくありません");
            }

            // 2. 有効チェック
            if (!boUser.getIsActive()) {
                throw new BusinessException("BO_USER_INACTIVE", "このアカウントは無効化されています");
            }

            // 3. トークン発行
            BoAuthService.TokenPair tokenPair = boAuthService.createToken(boUser);

            // 4. 最終ログイン日時を更新
            boUser.setLastLoginAt(LocalDateTime.now());

            // 5. ログイン成功を記録
            operationHistoryService.logLoginSuccess(boUser);

            // 6. レスポンス
            BoAuthResponse response = new BoAuthResponse(
                    BoUserDto.fromEntity(boUser),
                    tokenPair.getRawToken(),
                    tokenPair.getAuthToken().getExpiresAt()
            );
            return ApiResponse.success(response);

        } catch (ResourceNotFoundException ex) {
            if ("BO_USER_NOT_FOUND".equals(ex.getErrorCode())) {
                operationHistoryService.logLoginFailure(request.getEmail());
                throw new BusinessException("INVALID_CREDENTIALS", "メールアドレスまたはパスワードが正しくありません");
            }
            throw ex;
        }
    }

    /**
     * BoUser ログアウト
     */
    @PostMapping("/logout")
    public ApiResponse<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        boAuthService.revokeToken(token);
        return ApiResponse.success(Map.of("message", "ログアウトしました"));
    }

    /**
     * BoUser 情報取得
     */
    @GetMapping("/me")
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
