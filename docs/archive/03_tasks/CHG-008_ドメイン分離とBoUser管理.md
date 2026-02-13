# CHG-008: ドメイン分離とBoUser管理 - 実装タスク

## 検証コマンド

```bash
# バックエンド（テーブル作成後にアプリ起動）
cd backend
./mvnw spring-boot:run

# フロントエンド（管理画面）
cd frontend
npm run dev

# API テスト（BoUser ログイン）
curl -X POST http://localhost:8080/api/bo-auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'

# API テスト（管理APIアクセス）
curl -X GET http://localhost:8080/api/bo/admin/members \
  -H "Authorization: Bearer <token>"

# 誤アクセス防止テスト（顧客トークンで管理API）
curl -X GET http://localhost:8080/api/bo/admin/members \
  -H "Authorization: Bearer <customer_token>"
# → 期待: 401 or 403
```

---

## Phase 1: バックエンド基盤整備

### Task 1-1: データベースマイグレーション（テーブル作成）

**目的**: `bo_users` / `bo_auth_tokens` テーブルを作成し、既存 `users` テーブルから `role` カラムを削除する。

#### 作業内容

**ファイル**: `backend/src/main/resources/db/migration/V009__create_bo_users_tables.sql`（新規作成）

```sql
-- bo_users テーブル作成
CREATE TABLE bo_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    permission_level VARCHAR(50) NOT NULL DEFAULT 'OPERATOR',
    last_login_at DATETIME,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bo_users_email ON bo_users(email);
CREATE INDEX idx_bo_users_is_active ON bo_users(is_active);

-- bo_auth_tokens テーブル作成
CREATE TABLE bo_auth_tokens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bo_user_id INTEGER NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bo_user_id) REFERENCES bo_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_bo_auth_tokens_token_hash ON bo_auth_tokens(token_hash);
CREATE INDEX idx_bo_auth_tokens_bo_user_id ON bo_auth_tokens(bo_user_id);
CREATE INDEX idx_bo_auth_tokens_expires_at ON bo_auth_tokens(expires_at);

-- 既存 User (Role=ADMIN) を bo_users に移行
INSERT INTO bo_users (email, password_hash, display_name, permission_level, is_active, created_at, updated_at)
SELECT email, password_hash, display_name, 'ADMIN', is_active, created_at, updated_at
FROM users
WHERE role = 'ADMIN';

-- 管理者レコードを users から削除
DELETE FROM users WHERE role = 'ADMIN';

-- users テーブルから role カラムを削除（SQLite はテーブル再作成が必要）
CREATE TABLE users_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users_new (id, email, password_hash, display_name, is_active, created_at, updated_at)
SELECT id, email, password_hash, display_name, is_active, created_at, updated_at
FROM users;

DROP TABLE users;
ALTER TABLE users_new RENAME TO users;

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active);
```

**検証**:
```bash
# アプリ起動時にマイグレーションが自動実行される
./mvnw spring-boot:run

# データベースを確認
sqlite3 data/ec.db
.tables
# → bo_users, bo_auth_tokens が存在することを確認
SELECT * FROM bo_users;
```

---

### Task 1-2: PermissionLevel enum 作成

**ファイル**: `backend/src/main/java/com/example/aiec/entity/PermissionLevel.java`（新規作成）

**参考**: `backend/src/main/java/com/example/aiec/entity/Role.java`（既存、削除予定）

```java
package com.example.aiec.entity;

public enum PermissionLevel {
    SUPER_ADMIN,  // スーパー管理者（全権限）
    ADMIN,        // 管理者（BoUser 管理以外の全権限）
    OPERATOR      // オペレーター（参照権限のみ）
}
```

---

### Task 1-3: BoUser エンティティ作成

**ファイル**: `backend/src/main/java/com/example/aiec/entity/BoUser.java`（新規作成）

**参考**: `backend/src/main/java/com/example/aiec/entity/User.java`

```java
package com.example.aiec.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "bo_users")
@Data
public class BoUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 50)
    private PermissionLevel permissionLevel = PermissionLevel.OPERATOR;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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

### Task 1-4: BoAuthToken エンティティ作成

**ファイル**: `backend/src/main/java/com/example/aiec/entity/BoAuthToken.java`（新規作成）

**参考**: `backend/src/main/java/com/example/aiec/entity/AuthToken.java`（存在する場合）

```java
package com.example.aiec.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "bo_auth_tokens")
@Data
public class BoAuthToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bo_user_id", nullable = false)
    private Long boUserId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

---

### Task 1-5: BoUserRepository 作成

**ファイル**: `backend/src/main/java/com/example/aiec/repository/BoUserRepository.java`（新規作成）

**参考**: `backend/src/main/java/com/example/aiec/repository/UserRepository.java`

```java
package com.example.aiec.repository;

import com.example.aiec.entity.BoUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BoUserRepository extends JpaRepository<BoUser, Long> {
    Optional<BoUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

---

### Task 1-6: BoAuthTokenRepository 作成

**ファイル**: `backend/src/main/java/com/example/aiec/repository/BoAuthTokenRepository.java`（新規作成）

```java
package com.example.aiec.repository;

import com.example.aiec.entity.BoAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BoAuthTokenRepository extends JpaRepository<BoAuthToken, Long> {
    Optional<BoAuthToken> findByTokenHash(String tokenHash);
}
```

---

### Task 1-7: BoUserService 作成

**ファイル**: `backend/src/main/java/com/example/aiec/service/BoUserService.java`（新規作成）

**参考**: `backend/src/main/java/com/example/aiec/service/UserService.java`

```java
package com.example.aiec.service;

import com.example.aiec.entity.BoUser;
import com.example.aiec.entity.PermissionLevel;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ResourceNotFoundException;
import com.example.aiec.repository.BoUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoUserService {
    private final BoUserRepository boUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * BoUser を作成
     */
    @Transactional
    public BoUser createBoUser(String email, String displayName, String password, PermissionLevel permissionLevel) {
        if (boUserRepository.existsByEmail(email)) {
            throw new BusinessException("BO_USER_ALREADY_EXISTS", "このメールアドレスは既に登録されています");
        }

        BoUser boUser = new BoUser();
        boUser.setEmail(email);
        boUser.setDisplayName(displayName);
        boUser.setPasswordHash(passwordEncoder.encode(password));
        boUser.setPermissionLevel(permissionLevel);
        boUser.setIsActive(true);

        return boUserRepository.save(boUser);
    }

    /**
     * メールアドレスで BoUser を検索
     */
    public BoUser findByEmail(String email) {
        return boUserRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("BO_USER_NOT_FOUND", "BoUserが見つかりません"));
    }

    /**
     * パスワード検証
     */
    public boolean verifyPassword(BoUser boUser, String rawPassword) {
        return passwordEncoder.matches(rawPassword, boUser.getPasswordHash());
    }

    /**
     * 全 BoUser 取得
     */
    public List<BoUser> findAll() {
        return boUserRepository.findAll();
    }

    /**
     * BoUser の状態変更
     */
    @Transactional
    public BoUser updateStatus(Long id, Boolean isActive) {
        BoUser boUser = boUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("BO_USER_NOT_FOUND", "BoUserが見つかりません"));
        boUser.setIsActive(isActive);
        return boUserRepository.save(boUser);
    }

    /**
     * BoUser の権限レベル変更
     */
    @Transactional
    public BoUser updatePermissionLevel(Long id, PermissionLevel permissionLevel) {
        BoUser boUser = boUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("BO_USER_NOT_FOUND", "BoUserが見つかりません"));
        boUser.setPermissionLevel(permissionLevel);
        return boUserRepository.save(boUser);
    }
}
```

---

### Task 1-8: BoAuthService 作成

**ファイル**: `backend/src/main/java/com/example/aiec/service/BoAuthService.java`（新規作成）

**参考**: `backend/src/main/java/com/example/aiec/service/AuthService.java`

```java
package com.example.aiec.service;

import com.example.aiec.entity.BoAuthToken;
import com.example.aiec.entity.BoUser;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ForbiddenException;
import com.example.aiec.exception.ResourceNotFoundException;
import com.example.aiec.repository.BoAuthTokenRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoAuthService {
    private final BoAuthTokenRepository boAuthTokenRepository;
    private final BoUserService boUserService;

    /**
     * トークン生成
     */
    @Transactional
    public TokenPair createToken(BoUser boUser) {
        // 1. UUID v4 でランダムなトークンを生成
        String rawToken = UUID.randomUUID().toString();

        // 2. SHA-256 でハッシュ化
        String tokenHash = hashToken(rawToken);

        // 3. 有効期限を設定（7日間）
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        // 4. bo_auth_tokens テーブルに保存
        BoAuthToken authToken = new BoAuthToken();
        authToken.setBoUserId(boUser.getId());
        authToken.setTokenHash(tokenHash);
        authToken.setExpiresAt(expiresAt);
        authToken.setIsRevoked(false);
        boAuthTokenRepository.save(authToken);

        // 5. 生トークンとハッシュのペアを返す
        return new TokenPair(rawToken, authToken);
    }

    /**
     * トークン検証
     */
    public BoUser verifyToken(String rawToken) {
        // 1. トークンをハッシュ化
        String tokenHash = hashToken(rawToken);

        // 2. bo_auth_tokens から検索
        BoAuthToken authToken = boAuthTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "無効なトークンです"));

        // 3. 失効チェック
        if (authToken.getIsRevoked()) {
            throw new BusinessException("TOKEN_REVOKED", "このトークンは失効しています");
        }

        // 4. 有効期限チェック
        if (authToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("TOKEN_EXPIRED", "トークンの有効期限が切れています");
        }

        // 5. BoUser を取得
        BoUser boUser = boUserService.findAll().stream()
            .filter(u -> u.getId().equals(authToken.getBoUserId()))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("BO_USER_NOT_FOUND", "BoUserが見つかりません"));

        // 6. 有効チェック
        if (!boUser.getIsActive()) {
            throw new ForbiddenException("BO_USER_INACTIVE", "このアカウントは無効化されています");
        }

        return boUser;
    }

    /**
     * トークン失効（ログアウト）
     */
    @Transactional
    public void revokeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        BoAuthToken authToken = boAuthTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "無効なトークンです"));
        authToken.setIsRevoked(true);
        boAuthTokenRepository.save(authToken);
    }

    /**
     * トークンをハッシュ化
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * トークンペア（生トークンとハッシュ）
     */
    @Data
    public static class TokenPair {
        private final String rawToken;
        private final BoAuthToken authToken;
    }
}
```

---

### Task 1-9: BoUserDto / BoAuthResponse 作成

**ファイル**: `backend/src/main/java/com/example/aiec/dto/BoUserDto.java`（新規作成）

**参考**: `backend/src/main/java/com/example/aiec/dto/UserDto.java`

```java
package com.example.aiec.dto;

import com.example.aiec.entity.BoUser;
import com.example.aiec.entity.PermissionLevel;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BoUserDto {
    private Long id;
    private String email;
    private String displayName;
    private PermissionLevel permissionLevel;
    private LocalDateTime lastLoginAt;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BoUserDto fromEntity(BoUser boUser) {
        BoUserDto dto = new BoUserDto();
        dto.setId(boUser.getId());
        dto.setEmail(boUser.getEmail());
        dto.setDisplayName(boUser.getDisplayName());
        dto.setPermissionLevel(boUser.getPermissionLevel());
        dto.setLastLoginAt(boUser.getLastLoginAt());
        dto.setIsActive(boUser.getIsActive());
        dto.setCreatedAt(boUser.getCreatedAt());
        dto.setUpdatedAt(boUser.getUpdatedAt());
        return dto;
    }
}
```

**ファイル**: `backend/src/main/java/com/example/aiec/dto/BoAuthResponse.java`（新規作成）

```java
package com.example.aiec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BoAuthResponse {
    private BoUserDto user;
    private String token;
    private LocalDateTime expiresAt;
}
```

**ファイル**: `backend/src/main/java/com/example/aiec/dto/BoLoginRequest.java`（新規作成）

```java
package com.example.aiec.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BoLoginRequest {
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;

    @NotBlank(message = "パスワードは必須です")
    private String password;
}
```

---

### Task 1-10: BoAuthController 作成

**ファイル**: `backend/src/main/java/com/example/aiec/controller/BoAuthController.java`（新規作成）

**参考**: `backend/src/main/java/com/example/aiec/controller/AuthController.java`

```java
package com.example.aiec.controller;

import com.example.aiec.dto.*;
import com.example.aiec.entity.BoUser;
import com.example.aiec.exception.BusinessException;
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

        } catch (BusinessException ex) {
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
    public ApiResponse<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        boAuthService.revokeToken(token);
        return ApiResponse.success(Map.of("message", "ログアウトしました"));
    }

    /**
     * BoUser 情報取得
     */
    @GetMapping("/me")
    public ApiResponse<BoUserDto> getCurrentBoUser(@RequestHeader("Authorization") String authHeader) {
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
```

---

### Task 1-11: OperationHistoryService に BoUser 対応を追加

**ファイル**: `backend/src/main/java/com/example/aiec/service/OperationHistoryService.java`（既存ファイル）

**変更箇所**: `logLoginSuccess` メソッドをオーバーロード

**挿入位置**: 既存の `logLoginSuccess(User user)` メソッドの後

```java
/**
 * BoUser のログイン成功を記録
 */
public void logLoginSuccess(BoUser boUser) {
    OperationHistory history = new OperationHistory();
    history.setOperationType("LOGIN_SUCCESS");
    history.setPerformedBy(boUser.getEmail());
    history.setRequestPath("/api/bo-auth/login");
    history.setDetails("BoUser login successful: " + boUser.getEmail());
    operationHistoryRepository.save(history);
}
```

---

## Phase 2: 管理 API の移行

### Task 2-1: AdminController を BoAdminController に変更

**ファイル**: `backend/src/main/java/com/example/aiec/controller/AdminController.java`（既存ファイル）

**変更内容**:
1. クラス名を `AdminController` → `BoAdminController` に変更（ファイル名も変更）
2. `@RequestMapping("/api/admin/members")` → `@RequestMapping("/api/bo/admin/members")`
3. `User` → `BoUser` に変更
4. `AuthService` → `BoAuthService` に変更

**変更後のファイル**: `backend/src/main/java/com/example/aiec/controller/BoAdminController.java`

**参考**: 既存の `AdminController.java` をベースに変更

```java
package com.example.aiec.controller;

import com.example.aiec.dto.ApiResponse;
import com.example.aiec.dto.MemberDetailDto;
import com.example.aiec.dto.UserDto;
import com.example.aiec.entity.BoUser;
import com.example.aiec.entity.PermissionLevel;
import com.example.aiec.entity.User;
import com.example.aiec.exception.ForbiddenException;
import com.example.aiec.exception.ResourceNotFoundException;
import com.example.aiec.repository.OrderRepository;
import com.example.aiec.repository.UserRepository;
import com.example.aiec.service.BoAuthService;
import com.example.aiec.service.OperationHistoryService;
import com.example.aiec.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        String token = extractToken(authHeader);
        BoUser boUser = boAuthService.verifyToken(token);
        requireAdmin(boUser, "/api/bo/admin/members/" + id);

        User member = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "会員が見つかりません"));

        Long totalOrders = orderRepository.countByUserId(id);
        Long totalAmount = orderRepository.sumTotalPriceByUserId(id).orElse(0L);

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
            @RequestHeader("Authorization") String authHeader,
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
```

**注意**: 既存の `AdminController.java` は削除または名前を変更する。

---

### Task 2-2: AdminInventoryController を BoAdminInventoryController に変更

**ファイル**: `backend/src/main/java/com/example/aiec/controller/AdminInventoryController.java`（既存ファイル）

**変更内容**:
1. クラス名を `AdminInventoryController` → `BoAdminInventoryController` に変更
2. `@RequestMapping("/api/admin/inventory")` → `@RequestMapping("/api/bo/admin/inventory")`
3. `User` → `BoUser` に変更
4. `AuthService` → `BoAuthService` に変更

**変更後のファイル**: `backend/src/main/java/com/example/aiec/controller/BoAdminInventoryController.java`

同様の変更を適用（Task 2-1 と同じパターン）。

---

### Task 2-3: User エンティティから role フィールドを削除

**ファイル**: `backend/src/main/java/com/example/aiec/entity/User.java`（既存ファイル）

**削除箇所**:
```java
// 以下のフィールドを削除
@Enumerated(EnumType.STRING)
@Column(name = "role", nullable = false, length = 50)
private Role role;
```

**削除後の影響**: `Role` enum への依存を全て削除。

---

### Task 2-4: Role.java を削除

**ファイル**: `backend/src/main/java/com/example/aiec/entity/Role.java`（削除）

削除理由: BoUser 側では `PermissionLevel` を使用するため、`Role` は不要。

---

## Phase 3: フロントエンド対応

### Task 3-1: BoAuth API 関数の追加

**ファイル**: `frontend/src/lib/api.ts`（既存ファイル）

**挿入位置**: 既存の `login()` / `register()` メソッドの後

```typescript
// ============================================
// BoAuth API (管理者認証)
// ============================================

export interface BoLoginRequest {
  email: string
  password: string
}

export interface BoUser {
  id: number
  email: string
  displayName: string
  permissionLevel: 'SUPER_ADMIN' | 'ADMIN' | 'OPERATOR'
  lastLoginAt?: string
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface BoAuthResponse {
  user: BoUser
  token: string
  expiresAt: string
}

/**
 * BoUser ログイン
 */
export async function boLogin(
  email: string,
  password: string
): Promise<ApiResponse<BoAuthResponse>> {
  return post<BoAuthResponse>('/bo-auth/login', { email, password })
}

/**
 * BoUser ログアウト
 */
export async function boLogout(): Promise<ApiResponse<{ message: string }>> {
  return post<{ message: string }>('/bo-auth/logout', {})
}

/**
 * BoUser 情報取得
 */
export async function getBoUser(): Promise<ApiResponse<BoUser>> {
  return get<BoUser>('/bo-auth/me')
}
```

---

### Task 3-2: BoAuthContext 作成

**ファイル**: `frontend/src/contexts/BoAuthContext.tsx`（新規作成）

**参考**: `frontend/src/contexts/AuthContext.tsx`

```typescript
import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import * as api from '../lib/api'

interface BoAuthContextType {
  boUser: api.BoUser | null
  loading: boolean
  error: string | null
  boLogin: (email: string, password: string) => Promise<void>
  boLogout: () => Promise<void>
  clearError: () => void
}

const BoAuthContext = createContext<BoAuthContextType | undefined>(undefined)

export function BoAuthProvider({ children }: { children: ReactNode }) {
  const [boUser, setBoUser] = useState<api.BoUser | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const token = localStorage.getItem('bo_token')
    if (token) {
      api.getBoUser().then((response) => {
        if (response.success && response.data) {
          setBoUser(response.data)
        } else {
          localStorage.removeItem('bo_token')
        }
      })
    }
  }, [])

  const boLogin = async (email: string, password: string) => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.boLogin(email, password)
      if (response.success && response.data) {
        localStorage.setItem('bo_token', response.data.token)
        setBoUser(response.data.user)
      } else {
        setError(response.error?.message || 'ログインに失敗しました')
      }
    } catch (err) {
      setError('ログインに失敗しました')
    } finally {
      setLoading(false)
    }
  }

  const boLogout = async () => {
    try {
      await api.boLogout()
    } finally {
      localStorage.removeItem('bo_token')
      setBoUser(null)
    }
  }

  const clearError = () => setError(null)

  return (
    <BoAuthContext.Provider value={{ boUser, loading, error, boLogin, boLogout, clearError }}>
      {children}
    </BoAuthContext.Provider>
  )
}

export function useBoAuth() {
  const context = useContext(BoAuthContext)
  if (!context) {
    throw new Error('useBoAuth must be used within BoAuthProvider')
  }
  return context
}
```

---

### Task 3-3: api.ts に BoUser トークンヘッダー設定を追加

**ファイル**: `frontend/src/lib/api.ts`（既存ファイル）

**変更箇所**: `request()` 関数内でトークンを設定する部分

**現在の実装**:
```typescript
const token = localStorage.getItem('token')
if (token) {
  headers.Authorization = `Bearer ${token}`
}
```

**変更後**:
```typescript
// 管理API (/bo/**) の場合は bo_token を使用
if (url.startsWith('/bo')) {
  const boToken = localStorage.getItem('bo_token')
  if (boToken) {
    headers.Authorization = `Bearer ${boToken}`
  }
} else {
  // 顧客API の場合は token を使用
  const token = localStorage.getItem('token')
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
}
```

---

### Task 3-4: AdminLayout に BoAuthProvider を追加

**ファイル**: `frontend/src/App.tsx`（既存ファイル）

**挿入位置**: `<AuthProvider>` の内側、`<AdminLayout>` をラップ

**変更前**:
```tsx
<Route path="/bo" element={<AdminLayout />}>
  <Route path="item" element={<AdminItemPage />} />
  <Route path="order" element={<AdminOrderPage />} />
  <Route path="inventory" element={<AdminInventoryPage />} />
  <Route path="members" element={<AdminMembersPage />} />
</Route>
```

**変更後**:
```tsx
import { BoAuthProvider } from './contexts/BoAuthContext'

// ...

<BoAuthProvider>
  <Route path="/bo" element={<AdminLayout />}>
    <Route path="item" element={<AdminItemPage />} />
    <Route path="order" element={<AdminOrderPage />} />
    <Route path="inventory" element={<AdminInventoryPage />} />
    <Route path="members" element={<AdminMembersPage />} />
  </Route>
</BoAuthProvider>
```

---

### Task 3-5: BoLoginPage 作成

**ファイル**: `frontend/src/pages/BoLoginPage.tsx`（新規作成）

**参考**: `frontend/src/pages/LoginPage.tsx`

```tsx
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useBoAuth } from '../contexts/BoAuthContext'

export default function BoLoginPage() {
  const navigate = useNavigate()
  const { boLogin, loading, error, clearError } = useBoAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    clearError()

    try {
      await boLogin(email, password)
      navigate('/bo/item') // ログイン成功後に管理画面へ
    } catch {
      // エラーは BoAuthContext で管理されている
    }
  }

  return (
    <div className="mx-auto max-w-md px-6 py-12">
      <h1 className="mb-8 text-center font-serif text-3xl tracking-wider">
        Admin Login
      </h1>

      {error && (
        <div className="mb-6 rounded border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-zinc-700 mb-2">
            Email
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="w-full rounded border border-zinc-300 px-4 py-3 text-sm focus:border-zinc-900 focus:outline-none"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-zinc-700 mb-2">
            Password
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="w-full rounded border border-zinc-300 px-4 py-3 text-sm focus:border-zinc-900 focus:outline-none"
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded bg-zinc-900 px-4 py-3 text-sm uppercase tracking-widest text-white hover:bg-zinc-800 disabled:opacity-50 transition-colors"
        >
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>
    </div>
  )
}
```

---

### Task 3-6: App.tsx にボLoginPageルートを追加

**ファイル**: `frontend/src/App.tsx`（既存ファイル）

**挿入位置**: 認証画面のルート群

```tsx
import BoLoginPage from './pages/BoLoginPage'

// ...

{/* 認証画面 */}
<Route path="/auth/login" element={<LoginPage />} />
<Route path="/auth/register" element={<RegisterPage />} />
<Route path="/bo/login" element={<BoLoginPage />} />
```

---

## Phase 4: テスト・検証

### Task 4-1: 誤アクセス防止テスト実施

**テストスクリプト**: `backend/src/test/integration/BoAuthSecurityTest.java`（新規作成）

```java
// Spring Boot の統合テストとして実装
// 1. 顧客トークンで /api/bo/** にアクセス → 401/403
// 2. BoUser トークンで /api/** にアクセス → 挙動確認
// 3. トークンなしでアクセス → 401
// 4. 失効トークンでアクセス → 401
```

**手動テスト**:
```bash
# 1. 顧客トークンを取得
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"password"}'

# 2. 顧客トークンで管理APIにアクセス
curl -X GET http://localhost:8080/api/bo/admin/members \
  -H "Authorization: Bearer <customer_token>"
# → 期待: 401 INVALID_TOKEN

# 3. BoUser トークンを取得
curl -X POST http://localhost:8080/api/bo-auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'

# 4. BoUser トークンで管理APIにアクセス
curl -X GET http://localhost:8080/api/bo/admin/members \
  -H "Authorization: Bearer <bo_token>"
# → 期待: 200 OK
```

---

### Task 4-2: キャッシュ制御の確認

**確認コマンド**:
```bash
curl -I http://localhost:8080/api/bo/admin/members \
  -H "Authorization: Bearer <bo_token>"

# レスポンスヘッダーに以下が含まれることを確認
# Cache-Control: no-store, no-cache, must-revalidate
# Pragma: no-cache
# Expires: 0
```

---

### Task 4-3: データベースマイグレーション検証

**検証コマンド**:
```bash
sqlite3 backend/data/ec.db

# bo_users テーブルの確認
SELECT * FROM bo_users;

# bo_auth_tokens テーブルの確認
SELECT * FROM bo_auth_tokens;

# users テーブルに role カラムがないことを確認
PRAGMA table_info(users);
```

---

## まとめ

### 実装完了チェックリスト

- [ ] Phase 1: バックエンド基盤整備（Task 1-1 〜 1-11）
- [ ] Phase 2: 管理 API の移行（Task 2-1 〜 2-4）
- [ ] Phase 3: フロントエンド対応（Task 3-1 〜 3-6）
- [ ] Phase 4: テスト・検証（Task 4-1 〜 4-3）

### 次のステップ

実装タスクが完了したら、以下を実施：
1. 統合テストの実施
2. セキュリティ監査
3. パフォーマンステスト
4. ドキュメント更新（SPEC.md、data-model.md）
5. CHG-008 のアーカイブ
