package com.example.aiec.modules.customer.adapter.rest;

import com.example.aiec.modules.customer.adapter.dto.AuthResponse;
import com.example.aiec.modules.customer.adapter.dto.LoginRequest;
import com.example.aiec.modules.customer.adapter.dto.RegisterRequest;
import com.example.aiec.modules.customer.adapter.dto.UpdateMyProfileRequest;
import com.example.aiec.modules.customer.adapter.dto.UserAddressDto;
import com.example.aiec.modules.customer.adapter.dto.UserDto;
import com.example.aiec.modules.shared.dto.ApiResponse;
import com.example.aiec.modules.customer.domain.entity.UserAddress;
import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.shared.exception.BusinessException;
import com.example.aiec.modules.customer.domain.service.AuthService;
import com.example.aiec.modules.customer.domain.service.UserProfileService;
import com.example.aiec.modules.customer.domain.service.UserService;
import com.example.aiec.modules.shared.outbox.application.OutboxEventPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final UserProfileService userProfileService;
    private final OutboxEventPublisher outboxEventPublisher;

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
                outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                        "operationType", "LOGIN_FAILURE",
                        "performedBy", request.getEmail(),
                        "requestPath", "/api/auth/login",
                        "details", "Login attempt failed"));
                throw new BusinessException("INVALID_CREDENTIALS", "メールアドレスまたはパスワードが正しくありません");
            }

            // 2. トークン発行（生のトークンとハッシュのペアを取得）
            AuthService.TokenPair tokenPair = authService.createToken(user);

            // 3. ログイン成功を記録
            outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                    "operationType", "LOGIN_SUCCESS",
                    "performedBy", user.getEmail(),
                    "requestPath", "/api/auth/login",
                    "details", "User logged in successfully"));

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
                outboxEventPublisher.publish("OPERATION_PERFORMED", null, Map.of(
                        "operationType", "LOGIN_FAILURE",
                        "performedBy", request.getEmail(),
                        "requestPath", "/api/auth/login",
                        "details", "Login attempt failed"));
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
        return ApiResponse.success(toUserDto(user));
    }

    /**
     * 会員情報更新
     */
    @PutMapping("/me")
    @Operation(summary = "会員情報更新", description = "認証済み会員の更新可能項目を更新")
    public ApiResponse<UserDto> updateMyProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UpdateMyProfileRequest request) {
        if (request.hasDisallowedFields()) {
            throw new BusinessException("INVALID_REQUEST", "更新対象外の項目が含まれています");
        }

        String token = extractToken(authHeader);
        User currentUser = authService.verifyToken(token);

        UserProfileService.ProfileUpdateCommand command = new UserProfileService.ProfileUpdateCommand();
        command.setDisplayName(request.getDisplayName());
        command.setFullName(request.getFullName());
        command.setPhoneNumber(request.getPhoneNumber());
        command.setBirthDate(request.getBirthDate());
        command.setNewsletterOptIn(request.getNewsletterOptIn());

        User updated = userProfileService.updateMyProfile(currentUser.getId(), command);
        return ApiResponse.success(toUserDto(updated));
    }

    /**
     * 住所追加
     */
    @PostMapping("/me/addresses")
    @Operation(summary = "住所追加", description = "認証済み会員の住所を追加")
    public ApiResponse<UserAddressDto> addMyAddress(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UpsertMyAddressRequest request) {
        if (request.hasDisallowedFields()) {
            throw new BusinessException("INVALID_REQUEST", "更新対象外の項目が含まれています");
        }

        String token = extractToken(authHeader);
        User currentUser = authService.verifyToken(token);
        UserAddress created = userProfileService.addMyAddress(currentUser.getId(), toAddressCommand(request));
        return ApiResponse.success(UserAddressDto.fromEntity(created));
    }

    /**
     * 住所更新
     */
    @PutMapping("/me/addresses/{addressId}")
    @Operation(summary = "住所更新", description = "認証済み会員の住所を更新")
    public ApiResponse<UserAddressDto> updateMyAddress(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long addressId,
            @Valid @RequestBody UpsertMyAddressRequest request) {
        if (request.hasDisallowedFields()) {
            throw new BusinessException("INVALID_REQUEST", "更新対象外の項目が含まれています");
        }

        String token = extractToken(authHeader);
        User currentUser = authService.verifyToken(token);
        UserAddress updated = userProfileService.updateMyAddress(currentUser.getId(), addressId, toAddressCommand(request));
        return ApiResponse.success(UserAddressDto.fromEntity(updated));
    }

    /**
     * 住所削除
     */
    @DeleteMapping("/me/addresses/{addressId}")
    @Operation(summary = "住所削除", description = "認証済み会員の住所を論理削除")
    public ApiResponse<Map<String, Boolean>> deleteMyAddress(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long addressId) {
        String token = extractToken(authHeader);
        User currentUser = authService.verifyToken(token);
        userProfileService.deleteMyAddress(currentUser.getId(), addressId);
        return ApiResponse.success(Map.of("success", true));
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

    private UserDto toUserDto(User user) {
        List<UserAddressDto> addresses = userProfileService.getAddresses(user.getId()).stream()
                .map(UserAddressDto::fromEntity)
                .collect(Collectors.toList());
        return UserDto.fromEntity(user, addresses);
    }

    private UserProfileService.AddressUpsertCommand toAddressCommand(UpsertMyAddressRequest request) {
        UserProfileService.AddressUpsertCommand command = new UserProfileService.AddressUpsertCommand();
        command.setLabel(request.getLabel());
        command.setRecipientName(request.getRecipientName());
        command.setRecipientPhoneNumber(request.getRecipientPhoneNumber());
        command.setPostalCode(request.getPostalCode());
        command.setPrefecture(request.getPrefecture());
        command.setCity(request.getCity());
        command.setAddressLine1(request.getAddressLine1());
        command.setAddressLine2(request.getAddressLine2());
        command.setIsDefault(request.getIsDefault());
        command.setAddressOrder(request.getAddressOrder());
        return command;
    }

    @lombok.Data
    public static class UpsertMyAddressRequest {
        private String label;
        private String recipientName;
        private String recipientPhoneNumber;
        private String postalCode;
        private String prefecture;
        private String city;
        private String addressLine1;
        private String addressLine2;
        private Boolean isDefault;
        private Integer addressOrder;
        private final java.util.Map<String, Object> disallowedFields = new java.util.LinkedHashMap<>();

        @com.fasterxml.jackson.annotation.JsonAnySetter
        void captureUnknownField(String key, Object value) {
            disallowedFields.put(key, value);
        }

        public boolean hasDisallowedFields() {
            return !disallowedFields.isEmpty();
        }
    }

}
