# CHG-006 Task5: ロール認可と監査（実装タスク）

要件: `docs/01_requirements/CHG-006_Task5_ロール認可と監査.md`
設計: `docs/02_designs/CHG-006_Task5_ロール認可と監査.md`
作成日: 2026-02-12

---

## 検証コマンド

### バックエンド検証
```bash
# バックエンドコンテナ起動
docker compose up -d backend

# コンパイル確認
docker compose exec backend ./mvnw compile

# アプリケーション起動
docker compose up -d

# 動作確認（一般会員登録）
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "password123",
    "displayName": "Customer User"
  }'
# レスポンスでrole: "CUSTOMER"を確認

# 動作確認（管理者でログイン - 事前にDB作成が必要）
# まずは一般会員で管理APIにアクセスして403エラーを確認

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "password123"
  }'
# レスポンスからtokenを取得（以降の例では<CUSTOMER_TOKEN>として参照）

# 動作確認（一般会員が商品更新を試みる → 403エラー）
curl -X PUT http://localhost:8080/api/item/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <CUSTOMER_TOKEN>" \
  -d '{
    "name": "Updated Product",
    "price": 2000,
    "isPublished": true
  }'
# レスポンス: 403 Forbidden

# 操作履歴確認（DBに直接アクセス）
docker compose exec backend sh -c "sqlite3 /app/data/ec.db 'SELECT * FROM operation_histories ORDER BY created_at DESC LIMIT 5;'"
```

### 管理者アカウント作成
```bash
# BCryptハッシュ生成（パスワード: adminpass123）
# オンラインツールで生成: https://bcrypt-generator.com/
# または、一時的にJavaコードで生成

# SQLite DBに管理者を直接INSERT
docker compose exec backend sh -c "sqlite3 /app/data/ec.db \"
INSERT INTO users (email, display_name, password_hash, role, created_at, updated_at)
VALUES ('admin@example.com', 'Admin User', '\$2a\$10\$YourBCryptHashHere', 'ADMIN', datetime('now'), datetime('now'));
\""

# 管理者でログイン
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "adminpass123"
  }'
# レスポンスからtokenを取得（以降の例では<ADMIN_TOKEN>として参照）

# 動作確認（管理者が商品更新 → 成功）
curl -X PUT http://localhost:8080/api/item/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "name": "Updated Product by Admin",
    "price": 2000,
    "isPublished": true
  }'
# レスポンス: 200 OK
```

### フロントエンド検証
```bash
# フロントエンドコンテナ起動
docker compose up -d frontend

# ブラウザで確認
# 1. http://localhost:5173/auth/register で一般会員登録
# 2. ヘッダーの表示名の横にバッジが表示されないこと（CUSTOMER）
# 3. ログアウト後、管理者でログイン
# 4. ヘッダーの表示名の横に赤い "ADMIN" バッジが表示されること
```

---

## バックエンド実装タスク

### Task 5-BE-1: Role enum 作成

**ファイル**: `backend/src/main/java/com/example/aiec/entity/Role.java`（新規作成）

**コード**:
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

**参考**: `backend/src/main/java/com/example/aiec/entity/Order.java`のOrderStatus enum

---

### Task 5-BE-2: User entity に role フィールド追加

**ファイル**: `backend/src/main/java/com/example/aiec/entity/User.java`

**挿入位置**: `passwordHash`フィールドの直後

**コード**:
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private Role role = Role.CUSTOMER;
```

**import追加**:
```java
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
```

**参考**: `backend/src/main/java/com/example/aiec/entity/Order.java`の`status`フィールド（enum使用例）

---

### Task 5-BE-3: OperationHistory entity 作成

**ファイル**: `backend/src/main/java/com/example/aiec/entity/OperationHistory.java`（新規作成）

**コード**:
```java
package com.example.aiec.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作履歴エンティティ
 */
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

**参考**: `backend/src/main/java/com/example/aiec/entity/Order.java` — エンティティの基本構造

---

### Task 5-BE-4: OperationHistoryRepository 作成

**ファイル**: `backend/src/main/java/com/example/aiec/repository/OperationHistoryRepository.java`（新規作成）

**コード**:
```java
package com.example.aiec.repository;

import com.example.aiec.entity.OperationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 操作履歴リポジトリ
 */
public interface OperationHistoryRepository extends JpaRepository<OperationHistory, Long> {
    // 基本的なCRUD操作のみ（検索機能は将来拡張）
}
```

**参考**: `backend/src/main/java/com/example/aiec/repository/OrderRepository.java` — リポジトリの基本構造

---

### Task 5-BE-5: OperationHistoryService 作成

**ファイル**: `backend/src/main/java/com/example/aiec/service/OperationHistoryService.java`（新規作成）

**コード**:
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

**参考**: `backend/src/main/java/com/example/aiec/service/OrderService.java` — サービスの基本構造

---

### Task 5-BE-6: ForbiddenException 作成

**ファイル**: `backend/src/main/java/com/example/aiec/exception/ForbiddenException.java`（新規作成）

**コード**:
```java
package com.example.aiec.exception;

/**
 * 認可エラー（403 Forbidden）
 */
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

**参考**: `backend/src/main/java/com/example/aiec/exception/BusinessException.java` — 例外クラスの構造

---

### Task 5-BE-7: GlobalExceptionHandler に ForbiddenException 処理追加

**ファイル**: `backend/src/main/java/com/example/aiec/exception/GlobalExceptionHandler.java`

**挿入位置**: 既存の`@ExceptionHandler`メソッド（`handleBusinessException`など）の直後

**コード**:
```java
/**
 * 認可エラー（403 Forbidden）
 */
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

**参考**: 同じファイル内の`handleBusinessException`メソッド

---

### Task 5-BE-8: AuthController に OperationHistoryService 依存関係追加

**ファイル**: `backend/src/main/java/com/example/aiec/controller/AuthController.java`

**挿入位置**: クラスの`@RequiredArgsConstructor`アノテーションの直後、既存フィールド（`userService`, `authService`）の末尾

**コード**:
```java
private final OperationHistoryService operationHistoryService;
```

**import追加**:
```java
import com.example.aiec.service.OperationHistoryService;
```

**参考**: 同じファイル内の既存フィールド宣言パターン

---

### Task 5-BE-9: AuthController の login メソッド修正（操作履歴記録）

**ファイル**: `backend/src/main/java/com/example/aiec/controller/AuthController.java`

**変更内容**: メソッド全体を置き換え

**変更前**:
```java
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
```

**変更後**:
```java
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
        if ("USER_NOT_FOUND".equals(ex.getCode())) {
            operationHistoryService.logLoginFailure(request.getEmail());
            throw new BusinessException("INVALID_CREDENTIALS", "メールアドレスまたはパスワードが正しくありません");
        }
        throw ex;
    }
}
```

**参考**: Task5設計ドキュメントの「5-8. AuthController 修正」

---

### Task 5-BE-10: ItemController に依存関係追加

**ファイル**: `backend/src/main/java/com/example/aiec/controller/ItemController.java`

**挿入位置**: クラスの`@RequiredArgsConstructor`アノテーションの直後、既存フィールド（`productService`）の末尾

**コード**:
```java
private final AuthService authService;
private final OperationHistoryService operationHistoryService;
```

**import追加**:
```java
import com.example.aiec.service.AuthService;
import com.example.aiec.service.OperationHistoryService;
import com.example.aiec.entity.User;
import com.example.aiec.entity.Role;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ForbiddenException;
```

**参考**: Task2の`AuthController`における依存関係注入パターン

---

### Task 5-BE-11: ItemController の updateProduct メソッド修正（認可チェック）

**ファイル**: `backend/src/main/java/com/example/aiec/controller/ItemController.java`

**変更内容**: メソッド全体を置き換え

**変更前**:
```java
@PutMapping("/{id}")
public ApiResponse<ProductDto> updateProduct(
        @PathVariable Long id,
        @Valid @RequestBody UpdateProductRequest request
) {
    ProductDto product = productService.updateProduct(id, request);
    return ApiResponse.success(product);
}
```

**変更後**:
```java
/**
 * 商品更新（管理用）
 * PUT /api/item/:id
 */
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

**参考**: Task4の`OrderController`における認証ヘッダー処理パターン

---

### Task 5-BE-12: ItemController にヘルパーメソッド追加

**ファイル**: `backend/src/main/java/com/example/aiec/controller/ItemController.java`

**挿入位置**: クラスの末尾（既存エンドポイントメソッドの後）

**コード**:
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

**参考**: Task4の`OrderController`における同様のヘルパーメソッド

---

### Task 5-BE-13: OrderController に依存関係追加

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**挿入位置**: クラスの`@RequiredArgsConstructor`アノテーションの直後、既存フィールドの末尾

**コード**:
```java
private final AuthService authService;
private final OperationHistoryService operationHistoryService;
```

**import追加**:
```java
import com.example.aiec.service.AuthService;
import com.example.aiec.service.OperationHistoryService;
import com.example.aiec.entity.User;
import com.example.aiec.entity.Role;
import com.example.aiec.exception.BusinessException;
import com.example.aiec.exception.ForbiddenException;
```

**注意**: AuthServiceとOperationHistoryServiceは既にTask4で追加されている可能性があります。その場合はOperationHistoryServiceのみ追加してください。

**参考**: Task5-BE-10の同様の追加パターン

---

### Task 5-BE-14: OrderController の confirmOrder メソッド修正

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**変更内容**: メソッド全体を置き換え

**変更前**:
```java
@PostMapping("/{id}/confirm")
public ApiResponse<OrderDto> confirmOrder(@PathVariable Long id) {
    OrderDto order = orderService.confirmOrder(id);
    return ApiResponse.success(order);
}
```

**変更後**:
```java
/**
 * 注文確認（管理者向け）
 * POST /api/order/:id/confirm
 */
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

**参考**: Task5-BE-11の認証・認可パターン

---

### Task 5-BE-15: OrderController の shipOrder メソッド修正

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**変更内容**: メソッド全体を置き換え

**変更前**:
```java
@PostMapping("/{id}/ship")
public ApiResponse<OrderDto> shipOrder(@PathVariable Long id) {
    OrderDto order = orderService.shipOrder(id);
    return ApiResponse.success(order);
}
```

**変更後**:
```java
/**
 * 注文発送（管理者向け）
 * POST /api/order/:id/ship
 */
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

**参考**: Task5-BE-14の同様のパターン

---

### Task 5-BE-16: OrderController の deliverOrder メソッド修正

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**変更内容**: メソッド全体を置き換え

**変更前**:
```java
@PostMapping("/{id}/deliver")
public ApiResponse<OrderDto> deliverOrder(@PathVariable Long id) {
    OrderDto order = orderService.deliverOrder(id);
    return ApiResponse.success(order);
}
```

**変更後**:
```java
/**
 * 注文配達完了（管理者向け）
 * POST /api/order/:id/deliver
 */
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

**参考**: Task5-BE-14の同様のパターン

---

### Task 5-BE-17: OrderController の getAllOrders メソッド修正

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**変更内容**: メソッド全体を置き換え

**変更前**:
```java
@GetMapping
public ApiResponse<java.util.List<OrderDto>> getAllOrders() {
    java.util.List<OrderDto> orders = orderService.getAllOrders();
    return ApiResponse.success(orders);
}
```

**変更後**:
```java
/**
 * 全注文取得（管理者向け）
 * GET /api/order
 */
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

**参考**: Task5-BE-14の同様のパターン

---

### Task 5-BE-18: OrderController にヘルパーメソッド追加

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**挿入位置**: クラスの末尾（既存エンドポイントメソッドの後）

**コード**:
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

**注意**: Task4で`extractToken`が既に追加されている可能性があります。その場合は`requireAdmin`のみ追加してください。

**参考**: Task5-BE-12の同様のヘルパーメソッド

---

### Task 5-BE-19: UserDto に role フィールド追加

**ファイル**: `backend/src/main/java/com/example/aiec/dto/UserDto.java`

**挿入位置1**: フィールド宣言部分（`displayName`の直後）

**コード**:
```java
private String role;
```

**挿入位置2**: `fromEntity`メソッド内（`setDisplayName`の直後）

**変更前**:
```java
public static UserDto fromEntity(User user) {
    UserDto dto = new UserDto();
    dto.setId(user.getId());
    dto.setEmail(user.getEmail());
    dto.setDisplayName(user.getDisplayName());
    dto.setCreatedAt(user.getCreatedAt());
    return dto;
}
```

**変更後**:
```java
public static UserDto fromEntity(User user) {
    UserDto dto = new UserDto();
    dto.setId(user.getId());
    dto.setEmail(user.getEmail());
    dto.setDisplayName(user.getDisplayName());
    dto.setRole(user.getRole().name());
    dto.setCreatedAt(user.getCreatedAt());
    return dto;
}
```

**参考**: 同じファイル内の既存フィールドパターン

---

## フロントエンド実装タスク

### Task 5-FE-1: User 型に role フィールド追加

**ファイル**: `frontend/src/types/api.ts`

**挿入位置**: `User`インターフェースの`displayName`フィールドの直後

**コード**:
```typescript
role: 'CUSTOMER' | 'ADMIN'
```

**参考**: 同じファイル内の既存型定義パターン

---

### Task 5-FE-2: AuthContext に isAdmin ヘルパー追加

**ファイル**: `frontend/src/contexts/AuthContext.tsx`

**変更内容1**: `AuthContextType`インターフェースに`isAdmin`を追加

**挿入位置**: `isAuthenticated`フィールドの直後

**コード**:
```typescript
isAdmin: boolean
```

**変更内容2**: Providerの実装に`isAdmin`を追加

**挿入位置**: `const isAuthenticated = !!user && !!token`の直後

**コード**:
```typescript
const isAdmin = user?.role === 'ADMIN'
```

**変更内容3**: Provider valueに`isAdmin`を追加

**変更前**:
```typescript
return (
  <AuthContext.Provider value={{ user, token, isAuthenticated, login, register, logout }}>
    {children}
  </AuthContext.Provider>
)
```

**変更後**:
```typescript
return (
  <AuthContext.Provider value={{ user, token, isAuthenticated, isAdmin, login, register, logout }}>
    {children}
  </AuthContext.Provider>
)
```

**参考**: 同じファイル内の既存Context実装パターン

---

### Task 5-FE-3: Layout に管理者バッジ表示追加

**ファイル**: `frontend/src/components/Layout.tsx`

**挿入位置**: ヘッダーの認証状態表示部分（`{user?.displayName}`の直後）

**変更前**:
```typescript
{isAuthenticated ? (
  <>
    <Link to="/order/history" className="hover:text-zinc-600 transition-colors">
      Orders
    </Link>
    <span className="text-xs text-zinc-700">{user?.displayName}</span>
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

**変更後**:
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

**参考**: 同じファイル内のTailwind CSSクラス使用パターン

---

## テスト手順

### 1. 認可チェック（一般会員）

```
1. フロントエンド起動: http://localhost:5173
2. 会員登録: /auth/register で新規会員を作成
3. ログイン後、ヘッダーに "ADMIN" バッジが表示されないこと
4. curlで商品更新APIにアクセス（上記検証コマンド参照）
5. 403 Forbiddenエラーが返ること
6. DBでoperation_historiesテーブルを確認
7. AUTHORIZATION_ERRORイベントが記録されていること
```

### 2. 認可チェック（管理者）

```
1. DBに管理者アカウントを作成（上記検証コマンド参照）
2. 管理者でログイン
3. ヘッダーに赤い "ADMIN" バッジが表示されること
4. curlで商品更新APIにアクセス
5. 200 OKで成功すること
6. DBでoperation_historiesテーブルを確認
7. ADMIN_ACTIONイベントが記録されていること
```

### 3. ログイン操作履歴

```
1. 正しいメールアドレスとパスワードでログイン
2. DBでoperation_historiesテーブルを確認
3. LOGIN_SUCCESSが記録されていること

4. 誤ったパスワードでログイン試行
5. DBでoperation_historiesテーブルを確認
6. LOGIN_FAILUREが記録されていること

7. 存在しないメールアドレスでログイン試行
8. エラーメッセージが「メールアドレスまたはパスワードが正しくありません」であること（アカウント存在判別不可）
9. DBでLOGIN_FAILUREが記録されていること
```

### 4. 管理API全体のテスト

```
1. 管理者で以下のAPIを実行:
   - POST /api/order/{id}/confirm
   - POST /api/order/{id}/ship
   - POST /api/order/{id}/deliver
   - GET /api/order
2. すべて200 OKで成功すること
3. DBでADMIN_ACTIONイベントが各操作分記録されていること

4. 一般会員で同じAPIを実行
5. すべて403 Forbiddenで失敗すること
6. DBでAUTHORIZATION_ERRORイベントが各操作分記録されていること
```

### 5. 後方互換性の確認

```
1. 既存の顧客機能が動作すること:
   - 会員登録（新規登録ユーザーはCUSTOMER）
   - ログイン
   - カート操作
   - 注文作成
   - 注文履歴
2. 既存のゲスト機能が動作すること:
   - カート操作
   - 注文作成
   - 注文詳細表示
```

---

## 完了条件

- [ ] バックエンドのコンパイルが成功する
- [ ] フロントエンドのビルドが成功する
- [ ] 新規会員登録時にroleが`CUSTOMER`になる
- [ ] 一般会員が管理APIを実行すると403エラーになる
- [ ] 管理者が管理APIを実行すると成功する
- [ ] ログイン成功・失敗が操作履歴に記録される
- [ ] 認可エラーが操作履歴に記録される
- [ ] 管理操作が操作履歴に記録される
- [ ] ログイン失敗時のエラーメッセージでアカウント存在が判別できない
- [ ] フロントエンドで管理者に "ADMIN" バッジが表示される
- [ ] 一般会員に "ADMIN" バッジが表示されない
- [ ] 既存の顧客・ゲスト機能が破壊されていない
