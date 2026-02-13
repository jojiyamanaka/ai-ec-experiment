package com.example.aiec.controller;

import com.example.aiec.dto.*;
import com.example.aiec.entity.User;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.service.AuthService;
import com.example.aiec.service.OperationHistoryService;
import com.example.aiec.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 認証コントローラー
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final OperationHistoryService operationHistoryService;

    /**
     * 会員登録
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // 1. ユーザー作成
        User user = userService.createUser(request.getEmail(), request.getDisplayName(), request.getPassword());

        // 2. トークン発行（生のトークンとハッシュのペアを取得）
        AuthService.TokenPair tokenPair = authService.createToken(user);

        // 3. レスポンス（生のトークンをクライアントに返す）
        AuthResponse response = new AuthResponse(
                UserDto.fromEntity(user),
                tokenPair.getRawToken(),
                tokenPair.getAuthToken().getExpiresAt()
        );
        return ApiResponse.success(response);
    }

    /**
     * ログイン
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            // 1. ユーザー検証
            User user = userService.findByEmail(request.getEmail());
            if (!userService.verifyPassword(user, request.getPassword())) {
                // ログイン失敗を記録
                operationHistoryService.logLoginFailure(request.getEmail());
                throw new BusinessException("INVALID_CREDENTIALS", "メールアドレスまたはパスワードが正しくありません");
            }

            // 2. トークン発行（生のトークンとハッシュのペアを取得）
            AuthService.TokenPair tokenPair = authService.createToken(user);

            // 3. ログイン成功を記録
            operationHistoryService.logLoginSuccess(user);

            // 4. レスポンス（生のトークンをクライアントに返す）
            AuthResponse response = new AuthResponse(
                    UserDto.fromEntity(user),
                    tokenPair.getRawToken(),
                    tokenPair.getAuthToken().getExpiresAt()
            );
            return ApiResponse.success(response);
        } catch (BusinessException ex) {
            // USER_NOT_FOUNDもINVALID_CREDENTIALSに統一（アカウント存在判別防止）
            if ("USER_NOT_FOUND".equals(ex.getErrorCode())) {
                operationHistoryService.logLoginFailure(request.getEmail());
                throw new BusinessException("INVALID_CREDENTIALS", "メールアドレスまたはパスワードが正しくありません");
            }
            throw ex;
        }
    }

    /**
     * ログアウト
     */
    @PostMapping("/logout")
    public ApiResponse<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        authService.revokeToken(token); // 失効処理（物理削除ではない）
        return ApiResponse.success(Map.of("message", "ログアウトしました"));
    }

    /**
     * 会員情報取得
     */
    @GetMapping("/me")
    public ApiResponse<UserDto> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        User user = authService.verifyToken(token);
        return ApiResponse.success(UserDto.fromEntity(user));
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
