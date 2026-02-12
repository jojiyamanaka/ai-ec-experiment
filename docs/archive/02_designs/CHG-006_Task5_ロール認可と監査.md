# CHG-006 Task5: ロール認可と監査（設計）

要件: `docs/01_requirements/CHG-006_Task5_ロール認可と監査.md`
作成日: 2026-02-12

---

## 1. 設計方針

会員機能に最低限の運用安全性を追加する最終ステップ。

- **ロールベース認可**: `CUSTOMER`と`ADMIN`の2ロールを定義し、管理APIはADMINのみ許可
- **パスワード保護**: 既に実装済み（BCryptPasswordEncoder）、追加作業なし
- **監査ログ**: ログイン、認可エラー、管理操作をAuditLogエンティティに記録
- **最小限のアプローチ**: Spring Securityの全機能は使わず、軽量な独自実装で済ます
- **後方互換性**: 既存機能を破壊しない

---

## 2. データモデル

### 2-1. ロール定義（Enum）

**ファイル**: `backend/src/main/java/com/example/aiec/entity/Role.java`（新規）

```java
package com.example.aiec.entity;

/**
 * ユーザーロール
 */
public enum Role {
    /**
     * 一般顧客
     */
    CUSTOMER,

    /**
     * 管理者
     */
    ADMIN
}
```

**説明**:
- シンプルなenumで実装（テーブル不要）
- 将来的にロールが増える可能性は低いため、enumで十分

### 2-2. User エンティティの拡張

**変更内容**: `role`フィールドを追加

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private Role role = Role.CUSTOMER; // デフォルトはCUSTOMER
```

**データパターン**:

| ケース | `role` | 説明 |
|--------|--------|------|
| 通常の会員登録 | `CUSTOMER` | デフォルト |
| 管理者アカウント | `ADMIN` | 手動で設定（登録時は不可） |

**注意**:
- 会員登録API（`/api/auth/register`）ではロール指定を受け付けない（常に`CUSTOMER`）
- 管理者アカウントは別途DBに直接INSERTまたは専用ツールで作成

### 2-3. 操作履歴エンティティ

**ファイル**: `backend/src/main/java/com/example/aiec/entity/OperationHistory.java`（新規）

```java
@Entity
@Table(name = "operation_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * イベント種別（LOGIN_SUCCESS, LOGIN_FAILURE, AUTHORIZATION_ERROR, ADMIN_ACTION）
     */
    @Column(nullable = false, length = 50)
    private String eventType;

    /**
     * イベント詳細（JSONまたは自由形式テキスト）
     */
    @Column(nullable = false, length = 500)
    private String details;

    /**
     * 対象ユーザーID（ログイン失敗時はnullの場合あり）
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 対象ユーザーのメールアドレス（ログイン失敗時の記録用）
     */
    @Column(length = 255)
    private String userEmail;

    /**
     * IPアドレス（将来拡張用、現在はnull）
     */
    @Column(length = 45)
    private String ipAddress;

    /**
     * リクエストパス（例: /api/order/123/ship）
     */
    @Column(length = 255)
    private String requestPath;

    /**
     * 発生日時
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

**イベント種別**:

| eventType | 説明 | 記録タイミング |
|-----------|------|--------------|
| `LOGIN_SUCCESS` | ログイン成功 | AuthController.login() 成功時 |
| `LOGIN_FAILURE` | ログイン失敗 | AuthController.login() 認証エラー時 |
| `AUTHORIZATION_ERROR` | 認可エラー | 管理APIに非管理者がアクセス時 |
| `ADMIN_ACTION` | 管理操作 | 管理APIの実行成功時 |

### 2-4. OperationHistoryRepository

**ファイル**: `backend/src/main/java/com/example/aiec/repository/OperationHistoryRepository.java`（新規）

```java
public interface OperationHistoryRepository extends JpaRepository<OperationHistory, Long> {
    // 基本的なCRUD操作のみ（検索機能は将来拡張）
}
```

---

## 3. 認可機構

### 3-1. 方針: カスタムアノテーション + AOPではなく、メソッド内チェック

**理由**:
- Spring Securityの全機能は過剰（セッション管理、CSRF等は不要）
- 軽量で理解しやすい実装
- 既存コードへの影響を最小化

**実装方法**:
1. Controllerメソッド内で`@RequestHeader("Authorization")`を受け取る
2. `AuthService.verifyToken()`でユーザーを取得
3. `user.getRole() == Role.ADMIN`をチェック
4. 非管理者なら`ForbiddenException`をスロー

### 3-2. ForbiddenException（新規例外）

**ファイル**: `backend/src/main/java/com/example/aiec/exception/ForbiddenException.java`（新規）

```java
public class ForbiddenException extends RuntimeException {
    private final String code;
    private final String message;

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

### 3-3. GlobalExceptionHandler の拡張

**追加内容**: `ForbiddenException` → 403レスポンス

```java
@ExceptionHandler(ForbiddenException.class)
@ResponseStatus(HttpStatus.FORBIDDEN)
public ApiResponse<Void> handleForbiddenException(ForbiddenException ex) {
    return ApiResponse.error(ex.getCode(), ex.getMessage());
}
```

---

## 4. API 設計

### 4-1. 管理API一覧（認可必須）

以下のエンドポイントは**ADMIN**ロール必須:

| エンドポイント | 説明 | Controller | メソッド |
|--------------|------|-----------|---------|
| `PUT /api/item/{id}` | 商品更新 | ItemController | updateProduct |
| `POST /api/order/{id}/confirm` | 注文確認 | OrderController | confirmOrder |
| `POST /api/order/{id}/ship` | 注文発送 | OrderController | shipOrder |
| `POST /api/order/{id}/deliver` | 注文配達完了 | OrderController | deliverOrder |
| `GET /api/order` | 全注文取得 | OrderController | getAllOrders |

### 4-2. 認可エラーレスポンス

**HTTP 403 Forbidden**:
```json
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "この操作を実行する権限がありません"
  }
}
```

### 4-3. 認証エラーレスポンス（既存）

**HTTP 401 Unauthorized**:
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "認証が必要です"
  }
}
```

---

## 5. バックエンド実装

### 5-1. Role enum 作成

**ファイル**: `backend/src/main/java/com/example/aiec/entity/Role.java`（新規）

上記「2-1. ロール定義」参照

### 5-2. User エンティティ修正

**ファイル**: `backend/src/main/java/com/example/aiec/entity/User.java`

**追加フィールド**:
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private Role role = Role.CUSTOMER;
```

**import追加**:
```java
import com.example.aiec.entity.Role;
```

### 5-3. OperationHistory エンティティ作成

**ファイル**: `backend/src/main/java/com/example/aiec/entity/OperationHistory.java`（新規）

上記「2-3. 操作履歴エンティティ」参照

### 5-4. OperationHistoryRepository 作成

**ファイル**: `backend/src/main/java/com/example/aiec/repository/OperationHistoryRepository.java`（新規）

上記「2-4. OperationHistoryRepository」参照

### 5-5. OperationHistoryService 作成

**ファイル**: `backend/src/main/java/com/example/aiec/service/OperationHistoryService.java`（新規）

```java
package com.example.aiec.service;

import com.example.aiec.entity.OperationHistory;
import com.example.aiec.entity.User;
import com.example.aiec.repository.OperationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作履歴サービス
 */
@Service
@RequiredArgsConstructor
public class OperationHistoryService {

    private final OperationHistoryRepository operationHistoryRepository;

    /**
     * ログイン成功を記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginSuccess(User user) {
        OperationHistory log = new OperationHistory();
        log.setEventType("LOGIN_SUCCESS");
        log.setDetails("User logged in successfully");
        log.setUserId(user.getId());
        log.setUserEmail(user.getEmail());
        operationHistoryRepository.save(log);
    }

    /**
     * ログイン失敗を記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginFailure(String email) {
        OperationHistory log = new OperationHistory();
        log.setEventType("LOGIN_FAILURE");
        log.setDetails("Login attempt failed");
        log.setUserEmail(email);
        operationHistoryRepository.save(log);
    }

    /**
     * 認可エラーを記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuthorizationError(User user, String requestPath) {
        OperationHistory log = new OperationHistory();
        log.setEventType("AUTHORIZATION_ERROR");
        log.setDetails("User attempted to access admin resource without permission");
        log.setUserId(user.getId());
        log.setUserEmail(user.getEmail());
        log.setRequestPath(requestPath);
        operationHistoryRepository.save(log);
    }

    /**
     * 管理操作を記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAdminAction(User user, String requestPath, String details) {
        OperationHistory log = new OperationHistory();
        log.setEventType("ADMIN_ACTION");
        log.setDetails(details);
        log.setUserId(user.getId());
        log.setUserEmail(user.getEmail());
        log.setRequestPath(requestPath);
        operationHistoryRepository.save(log);
    }
}
```

**注意**:
- `propagation = Propagation.REQUIRES_NEW`: 監査ログは親トランザクションが失敗しても記録される

### 5-6. ForbiddenException 作成

**ファイル**: `backend/src/main/java/com/example/aiec/exception/ForbiddenException.java`（新規）

上記「3-2. ForbiddenException」参照

### 5-7. GlobalExceptionHandler 修正

**ファイル**: `backend/src/main/java/com/example/aiec/exception/GlobalExceptionHandler.java`

**追加内容**:
```java
@ExceptionHandler(ForbiddenException.class)
@ResponseStatus(HttpStatus.FORBIDDEN)
public ApiResponse<Void> handleForbiddenException(ForbiddenException ex) {
    return ApiResponse.error(ex.getCode(), ex.getMessage());
}
```

**import追加**:
```java
import org.springframework.http.HttpStatus;
```

### 5-8. AuthController 修正（監査ログ追加）

**ファイル**: `backend/src/main/java/com/example/aiec/controller/AuthController.java`

**変更内容**:

1. **依存関係追加**:
```java
private final OperationHistoryService operationHistoryService;
```

2. **loginメソッド修正**:

```java
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
        if ("USER_NOT_FOUND".equals(ex.getCode())) {
            operationHistoryService.logLoginFailure(request.getEmail());
            throw new BusinessException("INVALID_CREDENTIALS", "メールアドレスまたはパスワードが正しくありません");
        }
        throw ex;
    }
}
```

### 5-9. ItemController 修正（認可追加）

**ファイル**: `backend/src/main/java/com/example/aiec/controller/ItemController.java`

**変更内容**:

1. **依存関係追加**:
```java
private final AuthService authService;
private final OperationHistoryService operationHistoryService;
```

2. **updateProductメソッド修正**:

```java
@PutMapping("/{id}")
public ApiResponse<ProductDto> updateProduct(
        @PathVariable Long id,
        @RequestHeader("Authorization") String authHeader,
        @Valid @RequestBody UpdateProductRequest request) {

    // 認証・認可チェック
    String token = extractToken(authHeader);
    User user = authService.verifyToken(token);
    requireAdmin(user, "/api/item/" + id);

    // 管理操作実行
    ProductDto product = productService.updateProduct(id, request);

    // 操作履歴記録
    operationHistoryService.logAdminAction(user, "/api/item/" + id,
        "Updated product: " + product.getName());

    return ApiResponse.success(product);
}
```

3. **ヘルパーメソッド追加**:

```java
/**
 * Authorizationヘッダーからトークンを抽出
 */
private String extractToken(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new BusinessException("UNAUTHORIZED", "認証が必要です");
    }
    return authHeader.substring(7);
}

/**
 * 管理者権限チェック
 */
private void requireAdmin(User user, String requestPath) {
    if (user.getRole() != Role.ADMIN) {
        operationHistoryService.logAuthorizationError(user, requestPath);
        throw new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません");
    }
}
```

**import追加**:
```java
import com.example.aiec.entity.User;
import com.example.aiec.entity.Role;
import com.example.aiec.service.AuthService;
import com.example.aiec.service.AuditService;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ForbiddenException;
```

### 5-10. OrderController 修正（認可追加）

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**変更内容**:

1. **依存関係追加**:
```java
private final AuthService authService;
private final OperationHistoryService operationHistoryService;
```

2. **confirmOrderメソッド修正**:

```java
@PostMapping("/{id}/confirm")
public ApiResponse<OrderDto> confirmOrder(
        @PathVariable Long id,
        @RequestHeader("Authorization") String authHeader) {

    // 認証・認可チェック
    String token = extractToken(authHeader);
    User user = authService.verifyToken(token);
    requireAdmin(user, "/api/order/" + id + "/confirm");

    // 管理操作実行
    OrderDto order = orderService.confirmOrder(id);

    // 操作履歴記録
    operationHistoryService.logAdminAction(user, "/api/order/" + id + "/confirm",
        "Confirmed order: " + order.getOrderNumber());

    return ApiResponse.success(order);
}
```

3. **shipOrderメソッド修正**:

```java
@PostMapping("/{id}/ship")
public ApiResponse<OrderDto> shipOrder(
        @PathVariable Long id,
        @RequestHeader("Authorization") String authHeader) {

    // 認証・認可チェック
    String token = extractToken(authHeader);
    User user = authService.verifyToken(token);
    requireAdmin(user, "/api/order/" + id + "/ship");

    // 管理操作実行
    OrderDto order = orderService.shipOrder(id);

    // 操作履歴記録
    operationHistoryService.logAdminAction(user, "/api/order/" + id + "/ship",
        "Shipped order: " + order.getOrderNumber());

    return ApiResponse.success(order);
}
```

4. **deliverOrderメソッド修正**:

```java
@PostMapping("/{id}/deliver")
public ApiResponse<OrderDto> deliverOrder(
        @PathVariable Long id,
        @RequestHeader("Authorization") String authHeader) {

    // 認証・認可チェック
    String token = extractToken(authHeader);
    User user = authService.verifyToken(token);
    requireAdmin(user, "/api/order/" + id + "/deliver");

    // 管理操作実行
    OrderDto order = orderService.deliverOrder(id);

    // 操作履歴記録
    operationHistoryService.logAdminAction(user, "/api/order/" + id + "/deliver",
        "Delivered order: " + order.getOrderNumber());

    return ApiResponse.success(order);
}
```

5. **getAllOrdersメソッド修正**:

```java
@GetMapping
public ApiResponse<java.util.List<OrderDto>> getAllOrders(
        @RequestHeader("Authorization") String authHeader) {

    // 認証・認可チェック
    String token = extractToken(authHeader);
    User user = authService.verifyToken(token);
    requireAdmin(user, "/api/order");

    // 管理操作実行
    java.util.List<OrderDto> orders = orderService.getAllOrders();

    // 操作履歴記録
    operationHistoryService.logAdminAction(user, "/api/order",
        "Retrieved all orders (count: " + orders.size() + ")");

    return ApiResponse.success(orders);
}
```

6. **ヘルパーメソッド追加**:

```java
/**
 * Authorizationヘッダーからトークンを抽出
 */
private String extractToken(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new BusinessException("UNAUTHORIZED", "認証が必要です");
    }
    return authHeader.substring(7);
}

/**
 * 管理者権限チェック
 */
private void requireAdmin(User user, String requestPath) {
    if (user.getRole() != Role.ADMIN) {
        operationHistoryService.logAuthorizationError(user, requestPath);
        throw new ForbiddenException("FORBIDDEN", "この操作を実行する権限がありません");
    }
}
```

**import追加**:
```java
import com.example.aiec.entity.User;
import com.example.aiec.entity.Role;
import com.example.aiec.service.AuthService;
import com.example.aiec.service.AuditService;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ForbiddenException;
```

### 5-11. UserDto 修正（role追加）

**ファイル**: `backend/src/main/java/com/example/aiec/dto/UserDto.java`

**追加フィールド**:
```java
private String role;
```

**fromEntityメソッド修正**:
```java
public static UserDto fromEntity(User user) {
    UserDto dto = new UserDto();
    dto.setId(user.getId());
    dto.setEmail(user.getEmail());
    dto.setDisplayName(user.getDisplayName());
    dto.setRole(user.getRole().name()); // 追加
    dto.setCreatedAt(user.getCreatedAt());
    return dto;
}
```

---

## 6. フロントエンド実装

### 6-1. 型定義の修正

**ファイル**: `frontend/src/types/api.ts`

**変更内容**: `User`型に`role`フィールド追加

```typescript
export interface User {
  id: number
  email: string
  displayName: string
  role: 'CUSTOMER' | 'ADMIN' // 追加
  createdAt: string
}
```

### 6-2. AuthContext の修正

**ファイル**: `frontend/src/contexts/AuthContext.tsx`

**変更内容**: `isAdmin`ヘルパー関数を追加

```typescript
export interface AuthContextType {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  isAdmin: boolean // 追加
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, displayName: string) => Promise<void>
  logout: () => Promise<void>
}
```

**実装**:
```typescript
const isAdmin = user?.role === 'ADMIN'

return (
  <AuthContext.Provider value={{ user, token, isAuthenticated, isAdmin, login, register, logout }}>
    {children}
  </AuthContext.Provider>
)
```

### 6-3. Layout の修正（管理者表示）

**ファイル**: `frontend/src/components/Layout.tsx`

**変更内容**: 管理者の場合は表示名の横にバッジを表示

```typescript
{isAuthenticated ? (
  <>
    <Link to="/order/history" className="hover:text-zinc-600 transition-colors">
      Orders
    </Link>
    <span className="text-xs text-zinc-700">
      {user?.displayName}
      {user?.role === 'ADMIN' && (
        <span className="ml-2 rounded bg-red-600 px-2 py-0.5 text-white text-xs">
          ADMIN
        </span>
      )}
    </span>
    <button
      onClick={logout}
      className="hover:text-zinc-600 transition-colors"
    >
      Logout
    </button>
  </>
) : (
  // ... 既存のLogin/Registerリンク
)}
```

---

## 7. 処理フロー

### 7-1. ログインフロー（監査ログあり）

```
ユーザー → POST /api/auth/login
  → AuthController.login():
    1. メールアドレスでユーザー検索
    2. パスワード検証
       → 失敗: auditService.logLoginFailure() → 401エラー
       → 成功: 次へ
    3. トークン発行
    4. auditService.logLoginSuccess()
    5. レスポンス返却
  → クライアント: トークンを localStorage に保存
```

### 7-2. 管理API実行フロー（認可あり）

```
管理者 → PUT /api/item/1 (Authorization: Bearer <token>)
  → ItemController.updateProduct():
    1. extractToken() → トークン抽出
    2. authService.verifyToken() → ユーザー取得
    3. requireAdmin() → ロールチェック
       → ADMIN以外: auditService.logAuthorizationError() → 403エラー
       → ADMIN: 次へ
    4. productService.updateProduct() → 商品更新
    5. auditService.logAdminAction() → 監査ログ記録
    6. レスポンス返却
```

### 7-3. 一般顧客が管理APIにアクセスした場合

```
一般顧客 → PUT /api/item/1 (Authorization: Bearer <token>)
  → ItemController.updateProduct():
    1. extractToken() → トークン抽出
    2. authService.verifyToken() → ユーザー取得（role=CUSTOMER）
    3. requireAdmin() → ロールチェック
       → user.role != ADMIN
       → auditService.logAuthorizationError() 記録
       → ForbiddenException スロー
  → GlobalExceptionHandler:
    → 403 Forbidden レスポンス
```

---

## 8. 既存パターンとの整合性

| 観点 | 既存パターン | CHG-006 Task5 |
|------|-------------|---------------|
| パスワード保護 | BCryptPasswordEncoder（実装済み） | 変更なし |
| 例外処理 | BusinessException → 400 | ForbiddenException → 403 追加 |
| 認証方式 | JWT風トークン（SHA-256ハッシュ） | 変更なし |
| DTO変換 | fromEntity() | UserDto に role 追加 |
| レスポンス形式 | ApiResponse | 同様 |

---

## 9. セキュリティ考慮事項

### 9-1. パスワード保護（既に実装済み）

- ✅ BCryptPasswordEncoderでハッシュ化
- ✅ 平文保存なし
- ✅ 認証失敗時にアカウント存在を判別できない（USER_NOT_FOUND → INVALID_CREDENTIALS に統一）

### 9-2. 認可

- ロールチェックは全管理APIで実施
- 非管理者のアクセス試行は監査ログに記録

### 9-3. 監査ログ

- `REQUIRES_NEW`トランザクションで確実に記録
- ログイン失敗、認可エラーも記録
- 将来的にIPアドレスも記録可能（現在はnull）

---

## 10. テスト観点

### 10-1. 認可

- [ ] **ケース1: 管理者が商品更新**
  - ADMINロールのユーザーが商品更新APIを実行
  - 成功（200 OK）
  - 監査ログにADMIN_ACTIONが記録される

- [ ] **ケース2: 一般顧客が商品更新**
  - CUSTOMERロールのユーザーが商品更新APIを実行
  - 403 Forbidden
  - 監査ログにAUTHORIZATION_ERRORが記録される

- [ ] **ケース3: 未認証ユーザーが商品更新**
  - Authorizationヘッダーなしで商品更新APIを実行
  - 401 Unauthorized

- [ ] **ケース4: 管理者が全注文取得**
  - ADMINロールのユーザーが全注文取得APIを実行
  - 成功（200 OK）
  - 全注文が取得できる

- [ ] **ケース5: 一般顧客が全注文取得**
  - CUSTOMERロールのユーザーが全注文取得APIを実行
  - 403 Forbidden

### 10-2. 監査ログ

- [ ] **ケース6: ログイン成功**
  - 正しいメールアドレスとパスワードでログイン
  - LOGIN_SUCCESSが記録される

- [ ] **ケース7: ログイン失敗（パスワード誤り）**
  - 正しいメールアドレスと誤ったパスワードでログイン
  - LOGIN_FAILUREが記録される

- [ ] **ケース8: ログイン失敗（アカウント存在しない）**
  - 存在しないメールアドレスでログイン
  - LOGIN_FAILUREが記録される
  - エラーメッセージは「メールアドレスまたはパスワードが正しくありません」（アカウント存在を判別できない）

- [ ] **ケース9: 管理操作の記録**
  - ADMINが注文を発送
  - ADMIN_ACTIONが記録される
  - details に "Shipped order: ORD-xxx" が含まれる

### 10-3. 後方互換性

- [ ] **ケース10: 既存の顧客機能が動作**
  - 会員登録、ログイン、カート操作、注文作成が正常動作
  - 新規登録ユーザーのロールは自動的にCUSTOMER

- [ ] **ケース11: UserDtoにroleが含まれる**
  - /api/auth/me でユーザー情報取得
  - レスポンスに role フィールドが含まれる

---

## 11. データ移行

### 11-1. 既存ユーザーへのデフォルトロール付与

既存のUsersテーブルにroleカラムを追加し、デフォルト値を`CUSTOMER`に設定。

**SQLマイグレーション（概念）**:
```sql
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER';
```

### 11-2. 管理者アカウントの作成

**方法1: SQL直接実行**:
```sql
-- 管理者アカウント作成（パスワード: adminpass123）
-- BCrypt hash for "adminpass123": $2a$10$...（実際のハッシュ値はアプリで生成）
INSERT INTO users (email, display_name, password_hash, role, created_at, updated_at)
VALUES ('admin@example.com', 'Admin User', '$2a$10$...', 'ADMIN', NOW(), NOW());
```

**方法2: 専用エンドポイント（本タスクでは対象外）**:
- Task6以降で実装
- `/api/admin/users/{id}/role`でロール変更API作成

---

## 12. 今後の拡張（対象外）

本タスク（Task5）では実装しない機能:

- **ロールの追加**: SUPER_ADMIN、MODERATOR等
- **権限の細分化**: 商品管理専用、注文管理専用等
- **監査ログ検索API**: 管理画面から監査ログを検索
- **IPアドレス記録**: 現在はnull、将来的にHttpServletRequestから取得
- **二要素認証**: TOTP等
- **外部IDプロバイダ連携**: OAuth2.0、SAML等

---

## 13. エラーメッセージ一覧

| エラーコード | HTTPステータス | メッセージ | 発生条件 |
|------------|--------------|----------|----------|
| `UNAUTHORIZED` | 401 | 認証が必要です | トークンなし・無効 |
| `FORBIDDEN` | 403 | この操作を実行する権限がありません | 非管理者が管理APIにアクセス |
| `INVALID_CREDENTIALS` | 400 | メールアドレスまたはパスワードが正しくありません | ログイン失敗 |

---

## 14. 実装順序

1. **バックエンド基盤** (BE-1〜BE-7):
   - Role enum
   - User entity修正
   - OperationHistory entity
   - OperationHistoryRepository
   - OperationHistoryService
   - ForbiddenException
   - GlobalExceptionHandler修正

2. **認証・操作履歴** (BE-8):
   - AuthController修正（操作履歴追加）

3. **管理API認可** (BE-9〜BE-10):
   - ItemController修正
   - OrderController修正

4. **DTO修正** (BE-11):
   - UserDto修正

5. **フロントエンド** (FE-1〜FE-3):
   - 型定義修正
   - AuthContext修正
   - Layout修正

---

## 15. 完了条件

- [ ] 非管理者が管理APIを実行すると403エラーになる
- [ ] 管理者は既存の管理操作を継続利用できる
- [ ] パスワードは平文保存されていない（BCrypt）
- [ ] 認証失敗時にアカウント存在有無を判別できない
- [ ] 監査対象イベントがOperationHistoryテーブルに記録される
- [ ] Step1〜4の機能（会員登録、ログイン、カート、注文履歴）が破壊されていない
