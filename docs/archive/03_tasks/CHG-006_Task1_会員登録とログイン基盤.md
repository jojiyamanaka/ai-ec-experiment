# CHG-006 Task1: 会員登録とログイン基盤（タスク）

要件: `docs/01_requirements/CHG-006_Task1_会員登録とログイン基盤.md`
設計: `docs/02_designs/CHG-006_Task1_会員登録とログイン基盤.md`
作成日: 2026-02-12

検証コマンド:
- バックエンドコンパイル: `docker compose exec backend ./mvnw compile`
- バックエンドテスト: `docker compose exec backend ./mvnw test`
- コンテナ未起動の場合: `docker compose up -d` を先に実行
- データベース確認: `docker compose exec backend sqlite3 /app/data/ec.db`

---

## タスク一覧

### バックエンド実装

- [ ] **T-1**: `pom.xml` に Spring Security Crypto 依存関係を追加
- [ ] **T-2**: `User` エンティティを作成
- [ ] **T-3**: `AuthToken` エンティティを作成
- [ ] **T-4**: `UserRepository` を作成
- [ ] **T-5**: `AuthTokenRepository` を作成
- [ ] **T-6**: `RegisterRequest` DTO を作成
- [ ] **T-7**: `LoginRequest` DTO を作成
- [ ] **T-8**: `UserDto` DTO を作成
- [ ] **T-9**: `AuthResponse` DTO を作成
- [ ] **T-10**: `SecurityConfig` を作成
- [ ] **T-11**: `UserService` を作成
- [ ] **T-12**: `AuthService` を作成
- [ ] **T-13**: `AuthController` を作成
- [ ] **T-14**: `GlobalExceptionHandler` を修正

---

## T-1: pom.xml に Spring Security Crypto 依存関係を追加

パス: `backend/pom.xml`

`<dependencies>` セクションに追加（他の `<dependency>` と同じレベル）:

```xml
<!-- Spring Security Crypto (BCrypt for password hashing) -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

参考: 既存の依存関係と同じインデントで追加してください。

---

## T-2: User エンティティを作成

パス: `backend/src/main/java/com/example/aiec/entity/User.java`（新規作成）

参考: `backend/src/main/java/com/example/aiec/entity/Product.java`

```java
package com.example.aiec.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会員エンティティ
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
```

---

## T-3: AuthToken エンティティを作成

パス: `backend/src/main/java/com/example/aiec/entity/AuthToken.java`（新規作成）

参考: `backend/src/main/java/com/example/aiec/entity/Order.java`

```java
package com.example.aiec.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 認証トークンエンティティ
 */
@Entity
@Table(name = "auth_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean isRevoked = false;

    @Column
    private LocalDateTime revokedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * トークンの有効性チェック
     */
    public boolean isValid() {
        return !isRevoked && LocalDateTime.now().isBefore(expiresAt);
    }

    /**
     * トークンを失効させる
     */
    public void revoke() {
        this.isRevoked = true;
        this.revokedAt = LocalDateTime.now();
    }

}
```

---

## T-4: UserRepository を作成

パス: `backend/src/main/java/com/example/aiec/repository/UserRepository.java`（新規作成）

参考: `backend/src/main/java/com/example/aiec/repository/ProductRepository.java`

```java
package com.example.aiec.repository;

import com.example.aiec.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ユーザーリポジトリ
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * メールアドレスでユーザーを検索
     */
    Optional<User> findByEmail(String email);

    /**
     * メールアドレスの存在確認
     */
    boolean existsByEmail(String email);

}
```

---

## T-5: AuthTokenRepository を作成

パス: `backend/src/main/java/com/example/aiec/repository/AuthTokenRepository.java`（新規作成）

参考: `backend/src/main/java/com/example/aiec/repository/CartRepository.java`

```java
package com.example.aiec.repository;

import com.example.aiec.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 認証トークンリポジトリ
 */
@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    /**
     * トークンハッシュで検索
     */
    Optional<AuthToken> findByTokenHash(String tokenHash);

    /**
     * ユーザーの有効/失効トークンを取得
     */
    List<AuthToken> findByUserIdAndIsRevoked(Long userId, Boolean isRevoked);

    /**
     * 失効済みかつ期限切れのトークンを削除（定期クリーンアップ用）
     */
    void deleteByUserIdAndExpiresAtBeforeAndIsRevoked(Long userId, LocalDateTime expiresAt, Boolean isRevoked);

}
```

---

## T-6: RegisterRequest DTO を作成

パス: `backend/src/main/java/com/example/aiec/dto/RegisterRequest.java`（新規作成）

参考: `backend/src/main/java/com/example/aiec/dto/AddToCartRequest.java`

```java
package com.example.aiec.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会員登録リクエスト
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "メールアドレスの形式が正しくありません")
    private String email;

    @NotBlank(message = "表示名は必須です")
    @Size(min = 1, max = 100, message = "表示名は1〜100文字である必要があります")
    private String displayName;

    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, max = 100, message = "パスワードは8〜100文字である必要があります")
    private String password;

}
```

---

## T-7: LoginRequest DTO を作成

パス: `backend/src/main/java/com/example/aiec/dto/LoginRequest.java`（新規作成）

参考: `backend/src/main/java/com/example/aiec/dto/RegisterRequest.java`

```java
package com.example.aiec.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ログインリクエスト
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "メールアドレスの形式が正しくありません")
    private String email;

    @NotBlank(message = "パスワードは必須です")
    private String password;

}
```

---

## T-8: UserDto DTO を作成

パス: `backend/src/main/java/com/example/aiec/dto/UserDto.java`（新規作成）

参考: `backend/src/main/java/com/example/aiec/dto/ProductDto.java`

```java
package com.example.aiec.dto;

import com.example.aiec.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ユーザーDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;
    private String email;
    private String displayName;
    private LocalDateTime createdAt;

    /**
     * エンティティから DTO を生成
     */
    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getCreatedAt()
        );
    }

}
```

---

## T-9: AuthResponse DTO を作成

パス: `backend/src/main/java/com/example/aiec/dto/AuthResponse.java`（新規作成）

参考: `backend/src/main/java/com/example/aiec/dto/OrderDto.java`

```java
package com.example.aiec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 認証レスポンス（登録・ログイン時）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private UserDto user;
    private String token;
    private LocalDateTime expiresAt;

}
```

---

## T-10: SecurityConfig を作成

パス: `backend/src/main/java/com/example/aiec/config/SecurityConfig.java`（新規作成）

参考: `backend/src/main/java/com/example/aiec/config/WebConfig.java`

```java
package com.example.aiec.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * セキュリティ設定
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
```

---

## T-11: UserService を作成

パス: `backend/src/main/java/com/example/aiec/service/UserService.java`（新規作成）

参考: `backend/src/main/java/com/example/aiec/service/ProductService.java`

```java
package com.example.aiec.service;

import com.example.aiec.entity.User;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ConflictException;
import com.example.aiec.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ユーザーサービス
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * ユーザー作成
     */
    @Transactional
    public User createUser(String email, String displayName, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "このメールアドレスは既に登録されています");
        }

        User user = new User();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    /**
     * メールアドレスでユーザー検索
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "ユーザーが見つかりません"));
    }

    /**
     * パスワード検証
     */
    public boolean verifyPassword(User user, String password) {
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

}
```

---

## T-12: AuthService を作成

パス: `backend/src/main/java/com/example/aiec/service/AuthService.java`（新規作成）

参考: `backend/src/main/java/com/example/aiec/service/UserService.java`

```java
package com.example.aiec.service;

import com.example.aiec.entity.AuthToken;
import com.example.aiec.entity.User;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.repository.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 認証サービス
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthTokenRepository authTokenRepository;
    private static final int TOKEN_EXPIRATION_DAYS = 7;

    /**
     * トークン生成
     * @return AuthToken と生のトークン文字列のペア
     */
    @Transactional
    public TokenPair createToken(User user) {
        // 1. 生のトークン（UUID）を生成
        String rawToken = UUID.randomUUID().toString();

        // 2. ハッシュ化してDBに保存
        String tokenHash = hashToken(rawToken);

        AuthToken authToken = new AuthToken();
        authToken.setUser(user);
        authToken.setTokenHash(tokenHash);
        authToken.setExpiresAt(LocalDateTime.now().plusDays(TOKEN_EXPIRATION_DAYS));
        authTokenRepository.save(authToken);

        // 3. 生のトークンをクライアントに返すため、ペアで返す
        return new TokenPair(authToken, rawToken);
    }

    /**
     * トークン検証
     * @param rawToken クライアントから受け取った生のトークン
     * @return 認証済みユーザー
     */
    public User verifyToken(String rawToken) {
        // 1. トークンをハッシュ化
        String tokenHash = hashToken(rawToken);

        // 2. ハッシュ値でDB検索
        AuthToken authToken = authTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", "認証が必要です"));

        // 3. 有効性チェック（期限切れ・失効）
        if (!authToken.isValid()) {
            throw new BusinessException("UNAUTHORIZED",
                    authToken.getIsRevoked() ? "トークンが失効しています" : "トークンの有効期限が切れています");
        }

        return authToken.getUser();
    }

    /**
     * トークン失効（ログアウト）
     * @param rawToken クライアントから受け取った生のトークン
     */
    @Transactional
    public void revokeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        AuthToken authToken = authTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", "認証が必要です"));

        authToken.revoke();
        authTokenRepository.save(authToken);
    }

    /**
     * トークンをSHA-256でハッシュ化
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * トークンと生の文字列のペア
     */
    @Value
    public static class TokenPair {
        AuthToken authToken;
        String rawToken;
    }

}
```

---

## T-13: AuthController を作成

パス: `backend/src/main/java/com/example/aiec/controller/AuthController.java`（新規作成）

参考: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

```java
package com.example.aiec.controller;

import com.example.aiec.dto.*;
import com.example.aiec.entity.User;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.service.AuthService;
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
        // 1. ユーザー検証
        User user = userService.findByEmail(request.getEmail());
        if (!userService.verifyPassword(user, request.getPassword())) {
            throw new BusinessException("INVALID_CREDENTIALS", "メールアドレスまたはパスワードが正しくありません");
        }

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
     * ログアウト
     */
    @PostMapping("/logout")
    public ApiResponse<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        authService.revokeToken(token); // 失効処理（物理削除ではない）
        return ApiResponse.success(Map.of("message", "ログアウトしました"));
    }

    /**
     * 会員情報取得
     */
    @GetMapping("/me")
    public ApiResponse<UserDto> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
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
```

---

## T-14: GlobalExceptionHandler を修正

パス: `backend/src/main/java/com/example/aiec/exception/GlobalExceptionHandler.java`

`handleBusinessException()` メソッドを修正して、`UNAUTHORIZED` と `INVALID_CREDENTIALS` エラーコードを 401 ステータスで返すようにする。

既存の `handleBusinessException()` メソッドを以下に置き換え:

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
    // 認証エラーは 401 Unauthorized
    if ("UNAUTHORIZED".equals(ex.getErrorCode()) || "INVALID_CREDENTIALS".equals(ex.getErrorCode())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getErrorMessage()));
    }
    // その他のビジネス例外は 400 Bad Request
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getErrorMessage()));
}
```

---

## 検証手順

### 1. コンパイル確認

```bash
docker compose exec backend ./mvnw compile
```

エラーが出ないことを確認。

### 2. データベーステーブル確認

```bash
docker compose restart backend
docker compose exec backend sqlite3 /app/data/ec.db
```

SQLiteプロンプトで:

```sql
.tables
.schema users
.schema auth_tokens
.quit
```

`users` と `auth_tokens` テーブルが作成されていることを確認。

### 3. API動作確認

**会員登録**:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "displayName": "テストユーザー",
    "password": "password123"
  }'
```

期待結果: トークンとユーザー情報が返却される。

**ログイン**:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

期待結果: トークンとユーザー情報が返却される。

**会員情報取得**（トークンは登録/ログインで取得したものを使用）:

```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <取得したトークン>"
```

期待結果: ユーザー情報が返却される。

**ログアウト**:

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <取得したトークン>"
```

期待結果: `{"success": true, "data": {"message": "ログアウトしました"}}`

**ログアウト後の会員情報取得（失敗確認）**:

```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <同じトークン>"
```

期待結果: `{"success": false, "error": {"code": "UNAUTHORIZED", "message": "トークンが失効しています"}}`

---

## 注意事項

- すべてのファイルは UTF-8 エンコーディングで作成してください
- インデントは既存コードと統一してください（スペース4個）
- Lombok アノテーションは既存パターンに従ってください
- トークンのハッシュ化は SHA-256 を使用します（設計通り）
- ログアウト時は物理削除ではなく失効フラグを立てます（設計通り）
