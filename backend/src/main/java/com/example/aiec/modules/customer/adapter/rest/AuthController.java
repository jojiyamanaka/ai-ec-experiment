package com.example.aiec.modules.customer.adapter.rest;

import com.example.aiec.modules.customer.adapter.dto.AuthResponse;
import com.example.aiec.modules.customer.adapter.dto.LoginRequest;
import com.example.aiec.modules.customer.adapter.dto.RegisterRequest;
import com.example.aiec.modules.customer.adapter.dto.UserDto;
import com.example.aiec.modules.shared.dto.ApiResponse;
import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.customer.domain.service.AuthService;
import com.example.aiec.modules.customer.domain.service.UserService;
import com.example.aiec.modules.shared.event.OperationPerformedEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 認証コントローラー
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "顧客認証", description = "顧客の登録・ログイン・ログアウト")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 会員登録
     */
    @PostMapping("/register")
    @Operation(summary = "会員登録", description = "新規会員を登録しトークンを発行")
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
    @Operation(summary = "ログイン", description = "メールアドレスとパスワードで認証しトークンを発行")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            // 1. ユーザー検証
            User user = userService.findByEmail(request.getEmail());
            if (!userService.verifyPassword(user, request.getPassword())) {
                eventPublisher.publishEvent(new OperationPerformedEvent(
                        "LOGIN_FAILURE", request.getEmail(), "/api/auth/login", "Login attempt failed"));
                throw new BusinessException("INVALID_CREDENTIALS", "メールアドレスまたはパスワードが正しくありません");
            }

            // 2. トークン発行（生のトークンとハッシュのペアを取得）
            AuthService.TokenPair tokenPair = authService.createToken(user);

            // 3. ログイン成功を記録
            eventPublisher.publishEvent(new OperationPerformedEvent(
                    "LOGIN_SUCCESS", user.getEmail(), "/api/auth/login", "User logged in successfully"));

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
                eventPublisher.publishEvent(new OperationPerformedEvent(
                        "LOGIN_FAILURE", request.getEmail(), "/api/auth/login", "Login attempt failed"));
                throw new BusinessException("INVALID_CREDENTIALS", "メールアドレスまたはパスワードが正しくありません");
            }
            throw ex;
        }
    }

    /**
     * ログアウト
     */
    @PostMapping("/logout")
    @Operation(summary = "ログアウト", description = "認証トークンを失効させる")
    public ApiResponse<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        authService.revokeToken(token); // 失効処理（物理削除ではない）
        return ApiResponse.success(Map.of("message", "ログアウトしました"));
    }

    /**
     * 会員情報取得
     */
    @GetMapping("/me")
    @Operation(summary = "会員情報取得", description = "認証済み会員の情報を取得")
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
