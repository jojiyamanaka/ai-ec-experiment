# CHG-006 Task1: 会員登録とログイン基盤（設計）

要件: `docs/01_requirements/CHG-006_Task1_会員登録とログイン基盤.md`
作成日: 2026-02-12

---

## 1. 設計方針

既存のアーキテクチャとパターンを踏襲し、会員機能の基盤を構築する。

- **エンティティ層**: `User`（会員）と `AuthToken`（認証トークン）を追加
- **認証方式**: トークンベース認証（UUID を SHA-256 ハッシュ化してデータベースで管理）
- **パスワード**: BCrypt でハッシュ化（Spring Security の `BCryptPasswordEncoder` を使用）
- **トークン管理**:
  - クライアントには UUID（36文字）を返却
  - DB には SHA-256 ハッシュ（64文字）を保存
  - ログアウト時は失効フラグ（`isRevoked`）を立てる（物理削除しない）
- **トークン送信**: `Authorization: Bearer <token>` ヘッダー
- **セッション管理との関係**: 既存の `X-Session-Id`（ゲストカート用）と併用。認証後は `userId` とセッションIDを紐付ける（Task3 で実装）
- **例外処理**: `ConflictException`（メール重複）、`BusinessException`（認証失敗、バリデーションエラー）
- **DTO パターン**: `fromEntity()` 静的メソッド、`@Valid` バリデーション、`ApiResponse` レスポンス

---

## 2. データモデル

### 2-1. User エンティティ

```java
@Entity
@Table(name = "users")
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

    // @PrePersist, @PreUpdate でタイムスタンプ自動設定
}
```

### 2-2. AuthToken エンティティ

```java
@Entity
@Table(name = "auth_tokens")
public class AuthToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash; // SHA-256 ハッシュ（64文字）

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean isRevoked = false; // 失効フラグ

    @Column
    private LocalDateTime revokedAt; // 失効日時

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // トークンの有効性チェック
    public boolean isValid() {
        return !isRevoked && LocalDateTime.now().isBefore(expiresAt);
    }

    // トークンを失効させる
    public void revoke() {
        this.isRevoked = true;
        this.revokedAt = LocalDateTime.now();
    }
}
```

- **トークン管理**:
  - クライアントには UUID（36文字）を返却
  - DB には SHA-256 ハッシュ（64文字）を保存
  - セキュリティ: DB 侵害時もトークン平文が漏洩しない
- **トークン有効期限**: 7日間（定数で管理）
- **失効管理**: ログアウト時は物理削除ではなく `isRevoked = true` に更新
- **履歴保持**: 失効済みトークンもログイン履歴として保持（監査・不正アクセス検知に活用）

---

## 3. API 設計

### 3-1. 会員登録（REQ-1-001）

**エンドポイント**: `POST /api/auth/register`

**リクエスト**:
```json
{
  "email": "user@example.com",
  "displayName": "山田太郎",
  "password": "SecurePass123"
}
```

**レスポンス（成功）**:
```json
{
  "success": true,
  "data": {
    "user": {
      "id": 1,
      "email": "user@example.com",
      "displayName": "山田太郎",
      "createdAt": "2025-02-12T10:00:00Z"
    },
    "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "expiresAt": "2025-02-19T10:00:00Z"
  }
}
```

**エラーレスポンス**:
```json
// メールアドレス重複（409 Conflict）
{
  "success": false,
  "error": {
    "code": "EMAIL_ALREADY_EXISTS",
    "message": "このメールアドレスは既に登録されています"
  }
}

// バリデーションエラー（400 Bad Request）
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "メールアドレスの形式が正しくありません"
  }
}
```

### 3-2. ログイン（REQ-1-002）

**エンドポイント**: `POST /api/auth/login`

**リクエスト**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**レスポンス（成功）**:
```json
{
  "success": true,
  "data": {
    "user": {
      "id": 1,
      "email": "user@example.com",
      "displayName": "山田太郎",
      "createdAt": "2025-02-12T10:00:00Z"
    },
    "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "expiresAt": "2025-02-19T10:00:00Z"
  }
}
```

**エラーレスポンス**:
```json
// 認証失敗（401 Unauthorized）
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "メールアドレスまたはパスワードが正しくありません"
  }
}
```

### 3-3. ログアウト（REQ-1-003）

**エンドポイント**: `POST /api/auth/logout`

**ヘッダー**:
```
Authorization: Bearer a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

**レスポンス（成功）**:
```json
{
  "success": true,
  "data": {
    "message": "ログアウトしました"
  }
}
```

**エラーレスポンス**:
```json
// トークンなし・無効（401 Unauthorized）
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "認証が必要です"
  }
}
```

### 3-4. 会員情報取得（REQ-1-004）

**エンドポイント**: `GET /api/auth/me`

**ヘッダー**:
```
Authorization: Bearer a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

**レスポンス（成功）**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "displayName": "山田太郎",
    "createdAt": "2025-02-12T10:00:00Z"
  }
}
```

**エラーレスポンス**:
```json
// トークンなし・無効（401 Unauthorized）
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "認証が必要です"
  }
}
```

---

## 4. バックエンド実装

### 4-1. 新規追加: エンティティ

- `backend/src/main/java/com/example/aiec/entity/User.java`
- `backend/src/main/java/com/example/aiec/entity/AuthToken.java`

### 4-2. 新規追加: Repository

- `backend/src/main/java/com/example/aiec/repository/UserRepository.java`
  - `Optional<User> findByEmail(String email)`
  - `boolean existsByEmail(String email)`
- `backend/src/main/java/com/example/aiec/repository/AuthTokenRepository.java`
  - `Optional<AuthToken> findByTokenHash(String tokenHash)` — ハッシュ値で検索
  - `List<AuthToken> findByUserIdAndIsRevoked(Long userId, Boolean isRevoked)` — ユーザーの有効なトークン一覧
  - `void deleteByUserIdAndExpiresAtBeforeAndIsRevoked(Long userId, LocalDateTime now, Boolean isRevoked)` — 失効済み＋期限切れトークンの定期削除用

### 4-3. 新規追加: DTO

- `backend/src/main/java/com/example/aiec/dto/RegisterRequest.java`
  - `@NotBlank @Email private String email`
  - `@NotBlank @Size(min=1, max=100) private String displayName`
  - `@NotBlank @Size(min=8, max=100) private String password`
- `backend/src/main/java/com/example/aiec/dto/LoginRequest.java`
  - `@NotBlank @Email private String email`
  - `@NotBlank private String password`
- `backend/src/main/java/com/example/aiec/dto/AuthResponse.java`
  - `UserDto user`
  - `String token`
  - `LocalDateTime expiresAt`
- `backend/src/main/java/com/example/aiec/dto/UserDto.java`
  - `Long id, String email, String displayName, LocalDateTime createdAt`
  - `static UserDto fromEntity(User user)`

### 4-4. 新規追加: Service

**`UserService.java`** — ユーザー管理

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "ユーザーが見つかりません"));
    }

    public boolean verifyPassword(User user, String password) {
        return passwordEncoder.matches(password, user.getPasswordHash());
    }
}
```

**`AuthService.java`** — 認証・トークン管理

```java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthTokenRepository authTokenRepository;
    private static final int TOKEN_EXPIRATION_DAYS = 7;

    /**
     * トークン生成
     * @return AuthToken と生のトークン文字列のペア
     */
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
    @lombok.Value
    public static class TokenPair {
        AuthToken authToken;
        String rawToken;
    }
}
```

### 4-5. 新規追加: Controller

**`AuthController.java`**

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthService authService;

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

    @PostMapping("/logout")
    public ApiResponse<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        authService.revokeToken(token); // 失効処理（物理削除ではない）
        return ApiResponse.success(Map.of("message", "ログアウトしました"));
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        User user = authService.verifyToken(token);
        return ApiResponse.success(UserDto.fromEntity(user));
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("UNAUTHORIZED", "認証が必要です");
        }
        return authHeader.substring(7);
    }
}
```

### 4-6. 新規追加: Configuration

**`SecurityConfig.java`** — PasswordEncoder の Bean 登録

```java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

依存関係（`pom.xml`）:
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

### 4-7. 変更: GlobalExceptionHandler

401 Unauthorized を返すために、`UNAUTHORIZED` エラーコードの処理を追加:

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
    if ("UNAUTHORIZED".equals(ex.getErrorCode()) || "INVALID_CREDENTIALS".equals(ex.getErrorCode())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getErrorMessage()));
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ex.getErrorCode(), ex.getErrorMessage()));
}
```

---

## 5. フロントエンド実装

Task1 では API 基盤のみ構築。UI 実装は Task2 で対応。

---

## 6. 処理フロー

### 6-1. 会員登録フロー

```
ユーザー → POST /api/auth/register
  → @Valid: email/password バリデーション → 失敗 → 400 (INVALID_REQUEST)
  → UserService: メール重複チェック → 重複 → 409 (EMAIL_ALREADY_EXISTS)
  → UserService: パスワードハッシュ化（BCrypt） + User 作成
  → AuthService: トークン生成
    1. UUID 生成（生のトークン）
    2. SHA-256 ハッシュ化
    3. ハッシュを DB 保存（有効期限7日、isRevoked=false）
  → (OK) ユーザー情報 + 生のトークン返却
```

### 6-2. ログインフロー

```
ユーザー → POST /api/auth/login
  → @Valid: email/password バリデーション → 失敗 → 400 (INVALID_REQUEST)
  → UserService: メールでユーザー検索 → 不在 → 400 (USER_NOT_FOUND)
  → UserService: パスワード検証（BCrypt） → 不一致 → 401 (INVALID_CREDENTIALS)
  → AuthService: トークン生成
    1. UUID 生成（生のトークン）
    2. SHA-256 ハッシュ化
    3. ハッシュを DB 保存（有効期限7日、isRevoked=false）
  → (OK) ユーザー情報 + 生のトークン返却
```

### 6-3. ログアウトフロー

```
ユーザー → POST /api/auth/logout (Authorization: Bearer <token>)
  → AuthService: トークン検証
    1. 生のトークンを SHA-256 ハッシュ化
    2. ハッシュで DB 検索 → 不在 → 401 (UNAUTHORIZED)
    3. 有効性チェック（isRevoked, expiresAt） → 無効 → 401
  → AuthService: トークン失効
    1. isRevoked = true に更新
    2. revokedAt = 現在時刻 に設定
    （物理削除しない）
  → (OK) 成功メッセージ返却
```

### 6-4. 会員情報取得フロー

```
ユーザー → GET /api/auth/me (Authorization: Bearer <token>)
  → AuthService: トークン検証
    1. 生のトークンを SHA-256 ハッシュ化
    2. ハッシュで DB 検索 → 不在 → 401 (UNAUTHORIZED)
    3. 有効性チェック（isRevoked, expiresAt） → 無効 → 401
  → AuthService: トークンから User 取得
  → (OK) ユーザー情報返却
```

---

## 7. 既存パターンとの整合性

| 観点 | 既存パターン | CHG-006 Task1 |
|------|-------------|---------------|
| エンティティ | Product, Order, Cart | User, AuthToken |
| DTO変換 | `fromEntity()` 静的メソッド | 同様 |
| バリデーション | `@Valid` + `@NotNull` 等 | 同様（`@Email`, `@Size` 追加） |
| 例外処理 | `BusinessException`, `ConflictException` | 同様 |
| HTTPステータス | 400/404/409 | 同様 + 401（認証エラー） |
| レスポンス形式 | `ApiResponse.success/error` | 同様 |
| Repository | JpaRepository + カスタムメソッド | 同様 |
| Service層 | ビジネスロジック集約 | 同様 |

---

## 8. セキュリティ考慮事項

- **パスワードハッシュ化**: BCrypt でハッシュ化（レインボーテーブル攻撃対策）
- **トークン生成**: UUID（推測困難、十分なエントロピー）
- **トークンハッシュ化**: SHA-256 でハッシュ化して DB 保存
  - DB 侵害時もトークン平文が漏洩しない
  - パスワードと同等のセキュリティレベル
- **トークン失効管理**: 論理削除（`isRevoked` フラグ）
  - ログイン履歴として保持（監査・不正アクセス検知に活用）
  - 失効済みトークンは認証で即座に拒否
- **認証失敗**: メール・パスワードどちらが間違っているか特定できないメッセージ（ユーザー列挙攻撃対策）
- **トークン有効期限**: 7日間（セッションハイジャック対策）
- **HTTPS**: 本番環境では必須（トークン盗聴対策） — 別タスクで対応

### トークンのライフサイクル

1. **生成**: UUID 生成 → SHA-256 ハッシュ化 → DB 保存 → 生 UUID をクライアントに返却
2. **検証**: クライアントから受信 → SHA-256 ハッシュ化 → DB でハッシュ検索 → 有効性チェック
3. **失効**: `isRevoked = true` に更新（物理削除しない）
4. **クリーンアップ**: 定期的に失効済み＋期限切れレコードを物理削除（オプション）

---

## 9. テスト観点

### 9-1. 会員登録
- [ ] 正常登録 → トークン発行
- [ ] メール重複 → 409エラー
- [ ] 無効なメール形式 → 400エラー
- [ ] パスワード8文字未満 → 400エラー
- [ ] 登録後、パスワードがハッシュ化されていること（DB確認）
- [ ] 登録後、トークンがハッシュ化されて保存されていること（DB確認）

### 9-2. ログイン
- [ ] 正しい認証情報 → トークン発行
- [ ] 存在しないメール → 401エラー
- [ ] 間違ったパスワード → 401エラー
- [ ] メール大文字・小文字区別なし（オプション）
- [ ] ログイン後、トークンがハッシュ化されて保存されていること（DB確認）

### 9-3. ログアウト
- [ ] 有効なトークン → 成功
- [ ] ログアウト後、同じトークンは無効（401エラー）
- [ ] ログアウト後、トークンレコードが削除されていないこと（DB確認）
- [ ] ログアウト後、`isRevoked = true` になっていること（DB確認）
- [ ] トークンなし → 401エラー

### 9-4. 会員情報取得
- [ ] 有効なトークン → ユーザー情報取得
- [ ] トークンなし → 401エラー
- [ ] 無効なトークン → 401エラー
- [ ] 失効済みトークン → 401エラー
- [ ] 期限切れトークン → 401エラー

### 9-5. トークン有効期限
- [ ] 発行後7日間は有効
- [ ] 7日経過後は無効

### 9-6. トークンセキュリティ
- [ ] DB に保存されているトークンが平文ではないこと（SHA-256 ハッシュ）
- [ ] 同じトークンから生成されたハッシュは常に同じ値
- [ ] クライアントに返却されるトークンは UUID 形式（36文字）
- [ ] DB に保存されるハッシュは64文字（16進数）

---

## 10. 今後の拡張（対象外）

- **リフレッシュトークン**: 長期ログイン保持
- **メール検証**: 登録時のメール確認
- **パスワードリセット**: 忘れた場合の再設定
- **OAuth連携**: Google/GitHub等でのログイン
- **2要素認証**: セキュリティ強化
- **ログイン履歴**: 不正アクセス検知

これらは Task5 以降または別案件で検討。
