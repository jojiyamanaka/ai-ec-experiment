# CHG-006 Task3: ゲスト共存とカート引き継ぎ（実装タスク）

要件: `docs/01_requirements/CHG-006_Task3_ゲスト共存とカート引き継ぎ.md`
設計: `docs/02_designs/CHG-006_Task3_ゲスト共存とカート引き継ぎ.md`
作成日: 2026-02-12

---

## 検証コマンド

### バックエンド

```bash
cd backend

# コンパイル確認
./mvnw compile

# テスト実行
./mvnw test

# アプリケーション起動
./mvnw spring-boot:run
```

### フロントエンド

```bash
cd frontend

# TypeScriptビルド確認
npm run build

# ESLintチェック
npm run lint

# 開発サーバー起動
npm run dev
```

### 動作確認

1. **ゲストでカートに商品を追加**
   - http://localhost:5173 にアクセス
   - 商品一覧から商品をカートに追加

2. **会員登録**
   - ヘッダーの「Register」から会員登録
   - 登録成功後、カートの商品が引き継がれることを確認

3. **ログアウト → 再度ゲストでカート追加 → ログイン**
   - ログアウト
   - 別の商品をカートに追加
   - ログイン
   - ゲストカートと会員カートがマージされることを確認

4. **データベース確認**
   ```bash
   # SQLiteに接続
   sqlite3 backend/data/ec.db

   # カートテーブル確認（user_idカラムが追加されていること）
   .schema carts
   SELECT * FROM carts;

   # 仮引当テーブル確認（UNIQUE制約が追加されていること）
   .schema stock_reservations
   SELECT * FROM stock_reservations;
   ```

---

## 実装タスク一覧

### バックエンド
1. [StockReservationエンティティにUNIQUE制約を追加](#task-1-stockreservationエンティティにunique制約を追加)
2. [Cartエンティティにuserフィールドを追加](#task-2-cartエンティティにuserフィールドを追加)
3. [CartRepositoryにfindByUserIdを追加](#task-3-cartrepositoryにfindbyuseridを追加)
4. [StockReservationRepositoryにメソッド追加](#task-4-stockreservationrepositoryにメソッド追加)
5. [MergeCartRequest DTOの作成](#task-5-mergecartrequest-dtoの作成)
6. [MergeCartResponse DTOの作成](#task-6-mergecartresponse-dtoの作成)
7. [InventoryServiceにメソッド追加](#task-7-inventoryserviceにメソッド追加)
8. [CartServiceに依存関係を追加](#task-8-cartserviceに依存関係を追加)
9. [CartServiceにメソッド追加](#task-9-cartserviceにメソッド追加)
10. [OrderControllerに依存関係を追加](#task-10-ordercontrollerに依存関係を追加)
11. [OrderControllerにエンドポイント追加](#task-11-ordercontrollerにエンドポイント追加)

### フロントエンド
12. [型定義の追加](#task-12-型定義の追加)
13. [API関数の追加](#task-13-api関数の追加)
14. [AuthContextの変更](#task-14-authcontextの変更)
15. [CartContextの変更](#task-15-cartcontextの変更)

---

## バックエンド実装

### Task 1: StockReservationエンティティにUNIQUE制約を追加

**ファイル**: `backend/src/main/java/com/example/aiec/entity/StockReservation.java`

**挿入位置**: `@Table(name = "stock_reservations")` の直後

**変更前**:
```java
@Entity
@Table(name = "stock_reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {
```

**変更後**:
```java
@Entity
@Table(
    name = "stock_reservations",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_reservation_session_product_type",
        columnNames = {"session_id", "product_id", "type"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {
```

**参考**: 既存のエンティティ定義パターン

---

### Task 2: Cartエンティティにuserフィールドを追加

**ファイル**: `backend/src/main/java/com/example/aiec/entity/Cart.java`

**挿入位置**: `private String sessionId;` の直後

**追加コード**:
```java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
```

**import追加**:
```java
import com.example.aiec.entity.User;
```

**参考**: 既存の`@ManyToOne`関連付けパターン

---

### Task 3: CartRepositoryにfindByUserIdを追加

**ファイル**: `backend/src/main/java/com/example/aiec/repository/CartRepository.java`

**挿入位置**: `Optional<Cart> findBySessionId(String sessionId);` の直後

**追加コード**:
```java
    /**
     * 会員IDでカートを検索
     */
    Optional<Cart> findByUserId(Long userId);
```

**参考**: 既存の`findBySessionId`メソッド

---

### Task 4: StockReservationRepositoryにメソッド追加

**ファイル**: `backend/src/main/java/com/example/aiec/repository/StockReservationRepository.java`

**挿入位置**: インターフェース内の最後のメソッドの直後

**追加コード**:
```java
    /**
     * 有効な仮引当を全件取得（SELECT ... FOR UPDATE）
     * 並行制御のために排他ロックを使用
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM StockReservation r " +
           "WHERE r.sessionId = :sessionId " +
           "AND r.product.id = :productId " +
           "AND r.type = 'TENTATIVE' " +
           "AND r.expiresAt > :now")
    List<StockReservation> findAllActiveTentativeForUpdate(
        @Param("sessionId") String sessionId,
        @Param("productId") Long productId,
        @Param("now") LocalDateTime now
    );

    /**
     * 有効な仮引当を全件取得（通常の読み取り）
     */
    @Query("SELECT r FROM StockReservation r " +
           "WHERE r.sessionId = :sessionId " +
           "AND r.product.id = :productId " +
           "AND r.type = 'TENTATIVE' " +
           "AND r.expiresAt > :now")
    List<StockReservation> findAllActiveTentativeBySessionAndProduct(
        @Param("sessionId") String sessionId,
        @Param("productId") Long productId,
        @Param("now") LocalDateTime now
    );
```

**import追加**:
```java
import jakarta.persistence.LockModeType;
import jakarta.persistence.Lock;
```

**参考**: 既存の`@Query`メソッド（`findActiveTentative`など）

---

### Task 5: MergeCartRequest DTOの作成

**ファイル**: `backend/src/main/java/com/example/aiec/dto/MergeCartRequest.java`（新規作成）

**コード全体**:
```java
package com.example.aiec.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * カート引き継ぎリクエスト
 */
@Data
public class MergeCartRequest {
    @NotBlank(message = "ゲストセッションIDは必須です")
    private String guestSessionId;
}
```

**参考**: 既存のDTOパターン（`AddToCartRequest.java`など）

---

### Task 6: MergeCartResponse DTOの作成

**ファイル**: `backend/src/main/java/com/example/aiec/dto/MergeCartResponse.java`（新規作成）

**コード全体**:
```java
package com.example.aiec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * カート引き継ぎレスポンス
 */
@Data
@AllArgsConstructor
public class MergeCartResponse {
    private CartDto cart;
    private List<MergeWarning> warnings;
    private List<MergeError> errors;

    /**
     * 警告メッセージ
     */
    @Data
    @AllArgsConstructor
    public static class MergeWarning {
        private String code;
        private String message;
    }

    /**
     * エラー詳細
     */
    @Data
    @AllArgsConstructor
    public static class MergeError {
        private Long productId;
        private String productName;
        private Integer requestedQuantity;
        private Integer availableStock;
        private String reason;
    }
}
```

**参考**: 既存のDTOパターン（`CartDto.java`など）

---

### Task 7: InventoryServiceにメソッド追加

**ファイル**: `backend/src/main/java/com/example/aiec/service/InventoryService.java`

**挿入位置**: クラスの最後（`cleanupExpiredReservations`メソッドの直後）

**追加コード**:
```java
    /**
     * 仮引当を移行（既存予約がある場合はマージ）
     *
     * 【重要な設計判断】
     * - UNIQUE制約により(sessionId, productId, type)の重複を防止
     * - SELECT ... FOR UPDATE で並行制御
     * - TO側に既存予約がある場合は数量をマージ
     * - FROM側に複数の予約がある場合（バグやリトライ）でも全件処理
     *
     * @param fromSessionId ゲストセッションID
     * @param toSessionId 会員セッションID
     * @param productId 商品ID
     */
    @Transactional
    public void transferReservation(String fromSessionId, String toSessionId, Long productId) {
        LocalDateTime now = LocalDateTime.now();

        // FROM側の予約を取得（全件、排他ロック）
        List<StockReservation> fromReservations =
            reservationRepository.findAllActiveTentativeForUpdate(fromSessionId, productId, now);

        if (fromReservations.isEmpty()) {
            return; // 移行対象なし（既に削除済み or 存在しない）
        }

        // FROM側の合計数量を計算（複数行ある場合は合算）
        int totalQuantity = fromReservations.stream()
            .mapToInt(StockReservation::getQuantity)
            .sum();

        // TO側に既に予約があるかチェック（排他ロック）
        List<StockReservation> toReservations =
            reservationRepository.findAllActiveTentativeForUpdate(toSessionId, productId, now);

        if (!toReservations.isEmpty()) {
            // ケースA: TO側に既存予約あり → 数量をマージ
            // UNIQUE制約により通常は1件だが、念のため全件処理
            StockReservation toReservation = toReservations.get(0);
            toReservation.setQuantity(toReservation.getQuantity() + totalQuantity);
            toReservation.setExpiresAt(now.plusMinutes(RESERVATION_EXPIRY_MINUTES));
            reservationRepository.save(toReservation);

            // FROM側を全削除
            reservationRepository.deleteAll(fromReservations);

            // TO側の余剰分も削除（UNIQUE制約違反の事前データがあった場合）
            if (toReservations.size() > 1) {
                reservationRepository.deleteAll(toReservations.subList(1, toReservations.size()));
            }

        } else {
            // ケースB: TO側に予約なし → 1件だけ移行、残りは削除
            StockReservation firstReservation = fromReservations.get(0);
            firstReservation.setSessionId(toSessionId);
            firstReservation.setQuantity(totalQuantity); // 複数あれば合算
            firstReservation.setExpiresAt(now.plusMinutes(RESERVATION_EXPIRY_MINUTES));
            reservationRepository.save(firstReservation);

            // 残りがあれば削除（通常はないが、バグやリトライで複数行できていた場合）
            if (fromReservations.size() > 1) {
                reservationRepository.deleteAll(fromReservations.subList(1, fromReservations.size()));
            }
        }
    }

    /**
     * セッション全体の仮引当を一括移行
     * 会員カートが存在しない場合（ゲストカート昇格）に使用
     *
     * @param fromSessionId ゲストセッションID
     * @param toSessionId 会員セッションID
     */
    @Transactional
    public void transferAllReservations(String fromSessionId, String toSessionId) {
        LocalDateTime now = LocalDateTime.now();
        List<StockReservation> reservations =
            reservationRepository.findAllActiveTentativeBySession(fromSessionId, now);

        for (StockReservation reservation : reservations) {
            reservation.setSessionId(toSessionId);
            reservation.setExpiresAt(now.plusMinutes(RESERVATION_EXPIRY_MINUTES));
            reservationRepository.save(reservation);
        }
    }
```

**参考**: 既存の`createReservation`, `updateReservation`メソッド

---

### Task 8: CartServiceに依存関係を追加

**ファイル**: `backend/src/main/java/com/example/aiec/service/CartService.java`

**挿入位置**: `private final InventoryService inventoryService;` の直後

**追加コード**:
```java
    private final UserRepository userRepository;
    private final StockReservationRepository reservationRepository;
```

**import追加**:
```java
import com.example.aiec.repository.UserRepository;
import com.example.aiec.repository.StockReservationRepository;
import com.example.aiec.entity.User;
import com.example.aiec.dto.MergeCartResponse;
import java.util.UUID;
```

**参考**: 既存の依存関係注入パターン

---

### Task 9: CartServiceにメソッド追加

**ファイル**: `backend/src/main/java/com/example/aiec/service/CartService.java`

**挿入位置**: クラスの最後（`createCart`メソッドの直後）

**追加コード**:
```java
    /**
     * ゲストカートを会員カートへ引き継ぐ
     *
     * 【データ整合性の保証】
     * - @Transactional により全操作が原子的に実行
     * - UNIQUE制約により二重予約を防止
     * - SELECT ... FOR UPDATE により並行制御
     *
     * 【冪等性の保証】
     * - ゲストカートが既に存在しない場合は成功レスポンス
     * - 2回目以降の実行でもエラーにならない
     *
     * @param userId 会員ID（認証済み）
     * @param guestSessionId ゲストセッションID
     * @return 引き継ぎ後のカート、警告、エラーのレスポンス
     */
    @Transactional
    public MergeCartResponse mergeGuestCart(Long userId, String guestSessionId) {
        List<MergeCartResponse.MergeWarning> warnings = new ArrayList<>();
        List<MergeCartResponse.MergeError> errors = new ArrayList<>();

        // 1. ゲストカートを取得（存在しない場合は空のカート扱い）
        Optional<Cart> guestCartOpt = cartRepository.findBySessionId(guestSessionId);
        if (guestCartOpt.isEmpty() || guestCartOpt.get().getItems().isEmpty()) {
            // ゲストカートが空の場合は会員カートをそのまま返す（冪等性）
            Cart userCart = cartRepository.findByUserId(userId)
                    .orElseGet(() -> createUserCart(userId));
            return new MergeCartResponse(CartDto.fromEntity(userCart), warnings, errors);
        }

        Cart guestCart = guestCartOpt.get();

        // 2. 会員カートを取得または作成
        Cart userCart = cartRepository.findByUserId(userId).orElse(null);

        // 3. 会員カートが存在しない場合、ゲストカートを昇格
        if (userCart == null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "ユーザーが見つかりません"));

            // ゲストカートのuserIdを設定（sessionIdはそのまま）
            guestCart.setUser(user);
            cartRepository.save(guestCart);

            // 仮引当はsessionIdベースなので、そのまま維持（移行不要）
            // ※ ゲストカートのsessionIdが会員カートのsessionIdになる

            return new MergeCartResponse(CartDto.fromEntity(guestCart), warnings, errors);
        }

        // 4. 会員カートが存在する場合、商品をマージ
        for (CartItem guestItem : new ArrayList<>(guestCart.getItems())) {
            Product product = guestItem.getProduct();

            // 非公開商品はスキップ
            if (!product.getIsPublished()) {
                errors.add(new MergeCartResponse.MergeError(
                    product.getId(),
                    product.getName(),
                    guestItem.getQuantity(),
                    0,
                    "商品が非公開です"
                ));
                continue;
            }

            // 会員カートに同じ商品が存在するか確認
            Optional<CartItem> userItemOpt = cartItemRepository.findByCartAndProduct(userCart, product);

            int guestQuantity = guestItem.getQuantity();
            int userQuantity = userItemOpt.map(CartItem::getQuantity).orElse(0);
            int totalQuantity = guestQuantity + userQuantity;

            // 上限チェック（9個）
            if (totalQuantity > MAX_QUANTITY_PER_ITEM) {
                totalQuantity = MAX_QUANTITY_PER_ITEM;
                warnings.add(new MergeCartResponse.MergeWarning(
                    "QUANTITY_LIMITED",
                    String.format("商品「%s」の数量を上限の%d個に制限しました（元: %d個）",
                        product.getName(), MAX_QUANTITY_PER_ITEM, guestQuantity + userQuantity)
                ));
            }

            // 在庫チェック
            LocalDateTime now = LocalDateTime.now();
            Integer availableStock = reservationRepository.calculateAvailableStock(product.getId(), now);
            if (availableStock == null) {
                availableStock = product.getStock();
            }

            // 既存の会員カート仮引当分を考慮
            if (userItemOpt.isPresent()) {
                availableStock += userItemOpt.get().getQuantity();
            }

            if (totalQuantity > availableStock) {
                errors.add(new MergeCartResponse.MergeError(
                    product.getId(),
                    product.getName(),
                    totalQuantity,
                    availableStock,
                    "在庫不足"
                ));
                continue;
            }

            // 仮引当の処理
            try {
                if (userItemOpt.isPresent()) {
                    // ケースA: 会員カートに同じ商品がある
                    // → 会員側の仮引当を更新、ゲスト側を解放
                    inventoryService.updateReservation(userCart.getSessionId(), product.getId(), totalQuantity);
                    inventoryService.releaseReservation(guestSessionId, product.getId());

                    CartItem userItem = userItemOpt.get();
                    userItem.setQuantity(totalQuantity);
                    cartItemRepository.save(userItem);

                } else {
                    // ケースB: 会員カートに同じ商品がない
                    // → ゲスト側の仮引当をUPDATEで移行
                    inventoryService.transferReservation(guestSessionId, userCart.getSessionId(), product.getId());

                    CartItem newItem = new CartItem();
                    newItem.setCart(userCart);
                    newItem.setProduct(product);
                    newItem.setQuantity(totalQuantity);
                    userCart.addItem(newItem);
                    cartItemRepository.save(newItem);
                }
            } catch (Exception e) {
                errors.add(new MergeCartResponse.MergeError(
                    product.getId(),
                    product.getName(),
                    totalQuantity,
                    availableStock,
                    "仮引当に失敗しました: " + e.getMessage()
                ));
            }
        }

        // 5. ゲストカートの残存仮引当を全解放（念のため）
        inventoryService.releaseAllReservations(guestSessionId);

        // 6. ゲストカートを削除
        guestCart.getItems().clear();
        cartRepository.delete(guestCart);

        // 7. 会員カートを保存
        userCart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(userCart);

        return new MergeCartResponse(CartDto.fromEntity(userCart), warnings, errors);
    }

    /**
     * 会員カートを作成
     */
    private Cart createUserCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "ユーザーが見つかりません"));

        Cart cart = new Cart();
        cart.setSessionId("user-session-" + UUID.randomUUID().toString());
        cart.setUser(user);
        cart.setCreatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }
```

**参考**: 既存の`addToCart`, `clearCart`メソッド

---

### Task 10: OrderControllerに依存関係を追加

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**挿入位置**: `private final CartService cartService;` の直後

**追加コード**:
```java
    private final AuthService authService;
```

**import追加**:
```java
import com.example.aiec.service.AuthService;
import com.example.aiec.entity.User;
import com.example.aiec.dto.MergeCartRequest;
import com.example.aiec.dto.MergeCartResponse;
import java.util.HashMap;
import java.util.Map;
```

**参考**: 既存の依存関係注入パターン

---

### Task 11: OrderControllerにエンドポイント追加

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**挿入位置**: クラスの最後（既存エンドポイントの直後）

**追加コード**:
```java
    /**
     * ゲストカートを会員カートへ引き継ぐ
     */
    @PostMapping("/cart/merge")
    public ApiResponse<?> mergeCart(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody MergeCartRequest request) {

        // トークンからユーザーIDを取得
        String token = extractToken(authHeader);
        User user = authService.verifyToken(token);

        MergeCartResponse response = cartService.mergeGuestCart(user.getId(), request.getGuestSessionId());

        // エラーがある場合は一部失敗として返す
        if (!response.getErrors().isEmpty()) {
            return ApiResponse.builder()
                    .success(false)
                    .data(response.getCart())
                    .error(new ApiResponse.ApiError(
                        "PARTIAL_MERGE_FAILED",
                        "一部の商品を引き継げませんでした",
                        response.getErrors()
                    ))
                    .build();
        }

        // 警告のみの場合は成功
        Map<String, Object> result = new HashMap<>();
        result.put("cart", response.getCart());
        if (!response.getWarnings().isEmpty()) {
            result.put("warnings", response.getWarnings());
        }

        return ApiResponse.success(result);
    }

    /**
     * Authorizationヘッダーからトークンを抽出
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("UNAUTHORIZED", "認証が必要です");
        }
        return authHeader.substring(7);
    }
```

**参考**: 既存のエンドポイント（`getCart`, `addToCart`など）

---

## フロントエンド実装

### Task 12: 型定義の追加

**ファイル**: `frontend/src/types/api.ts`

**挿入位置**: ファイル末尾に追加

**追加コード**:
```typescript
// ============================================
// カート引き継ぎ関連の型定義
// ============================================

// カート引き継ぎリクエスト
export interface MergeCartRequest {
  guestSessionId: string
}

// カート引き継ぎ警告
export interface MergeWarning {
  code: string
  message: string
}

// カート引き継ぎエラー詳細
export interface MergeError {
  productId: number
  productName: string
  requestedQuantity: number
  availableStock: number
  reason: string
}

// カート引き継ぎレスポンス
export interface MergeCartResponse {
  cart: Cart
  warnings?: MergeWarning[]
}
```

**参考**: 既存の`Cart`, `Product`などの型定義パターン

---

### Task 13: API関数の追加

**ファイル**: `frontend/src/lib/api.ts`

**挿入位置**: ファイル末尾、`export { getSessionId }`の前に追加

**追加コード**:
```typescript
// ============================================
// カート引き継ぎ API
// ============================================

/**
 * ゲストカートを会員カートへ引き継ぐ
 */
export async function mergeCart(
  guestSessionId: string
): Promise<ApiResponse<MergeCartResponse>> {
  return fetchApi<MergeCartResponse>('/api/order/cart/merge', {
    method: 'POST',
    body: JSON.stringify({ guestSessionId }),
  })
}
```

**import追加**:
```typescript
import type {
  // ... 既存のインポート
  MergeCartRequest,
  MergeCartResponse,
  MergeWarning,
  MergeError,
} from '../types/api'
```

**参考**: 既存のAPI関数（`getCart`, `addToCart`など）

---

### Task 14: AuthContextの変更

**ファイル**: `frontend/src/contexts/AuthContext.tsx`

#### 14-1. stateの追加

**挿入位置**: `const [error, setError] = useState<string | null>(null)` の直後

**追加コード**:
```typescript
  const [isMerging, setIsMerging] = useState(false) // 二重実行ガード
```

#### 14-2. mergeGuestCart関数の追加

**挿入位置**: `clearError`関数の直後

**追加コード**:
```typescript
  /**
   * カート引き継ぎを実行
   * 【冪等性の保証】二重実行ガードを実装
   */
  const mergeGuestCart = async () => {
    if (isMerging) {
      console.warn('カート引き継ぎは既に実行中です')
      return // 二重実行を防止
    }

    const guestSessionId = localStorage.getItem('sessionId')
    if (!guestSessionId) {
      return // ゲストセッションIDがない場合はスキップ
    }

    setIsMerging(true)
    try {
      const response = await api.mergeCart(guestSessionId)

      if (response.success && response.data) {
        // 警告メッセージを表示（オプション）
        if (response.data.warnings && response.data.warnings.length > 0) {
          console.warn('カート引き継ぎ警告:', response.data.warnings)
          // ユーザーに警告を表示（UIで実装）
        }

        // カート状態を更新するためにカスタムイベントを発火
        window.dispatchEvent(new Event('cart:merged'))
      } else if (response.error?.code === 'PARTIAL_MERGE_FAILED') {
        console.error('一部の商品を引き継げませんでした:', response.error.details)
        // ユーザーにエラーメッセージを表示（UIで実装）

        // 一部成功の場合もカート状態を更新
        window.dispatchEvent(new Event('cart:merged'))
      }
    } catch (err) {
      console.error('カート引き継ぎエラー:', err)
      // エラーが発生してもログイン処理は継続
    } finally {
      setIsMerging(false)
    }
  }
```

#### 14-3. login関数の変更

**挿入位置**: `login`関数内、`localStorage.setItem('authUser', JSON.stringify(user))`の直後

**追加コード**:
```typescript
        // カート引き継ぎを実行
        await mergeGuestCart()
```

#### 14-4. register関数の変更

**挿入位置**: `register`関数内、`localStorage.setItem('authUser', JSON.stringify(user))`の直後

**追加コード**:
```typescript
        // カート引き継ぎを実行
        await mergeGuestCart()
```

**参考**: 既存の`login`, `register`, `logout`メソッド

---

### Task 15: CartContextの変更

**ファイル**: `frontend/src/contexts/CartContext.tsx`

**挿入位置**: 既存の`useEffect`の直後（`refreshCart`関数の前）

**追加コード**:
```typescript
  // カート引き継ぎイベントをリッスン
  useEffect(() => {
    const handleCartMerged = () => {
      refreshCart() // カート状態を再取得
    }
    window.addEventListener('cart:merged', handleCartMerged)
    return () => window.removeEventListener('cart:merged', handleCartMerged)
  }, [])
```

**参考**: 既存の`useEffect`パターン

---

## 実装の順序

1. **バックエンド（データモデル）**: Task 1-4
   - エンティティとRepositoryの変更
   - データベーススキーマが変更されるため最初に実装

2. **バックエンド（DTO）**: Task 5-6
   - リクエスト・レスポンスの型定義

3. **バックエンド（サービス層）**: Task 7-9
   - ビジネスロジックの実装
   - 仮引当移行ロジックが重要

4. **バックエンド（コントローラー）**: Task 10-11
   - APIエンドポイントの公開

5. **フロントエンド**: Task 12-15
   - バックエンドAPIが完成してから実装

---

## テスト手順

### 1. ゲストカートから会員カートへの引き継ぎ

1. **ゲストでカートに商品を追加**
   ```
   商品一覧 → 商品A を1個カートに追加
   商品一覧 → 商品B を2個カートに追加
   カート確認: 商品A × 1個, 商品B × 2個
   ```

2. **会員登録**
   ```
   ヘッダー → Register → 会員登録フォーム入力 → 送信
   登録成功 → トップページへリダイレクト
   ```

3. **カート確認**
   ```
   カートを開く
   期待結果: 商品A × 1個, 商品B × 2個が引き継がれている
   ```

4. **データベース確認**
   ```sql
   SELECT * FROM carts WHERE user_id IS NOT NULL;
   -- 会員カートが作成されていること

   SELECT * FROM stock_reservations;
   -- 仮引当が会員カートのsessionIdで作成されていること
   ```

### 2. 会員カートとゲストカートのマージ

1. **ログアウト**
   ```
   ヘッダー → Logout
   ```

2. **ゲストでカートに別の商品を追加**
   ```
   商品一覧 → 商品A を2個カートに追加（既に会員カートにある商品）
   商品一覧 → 商品C を1個カートに追加（新規商品）
   カート確認: 商品A × 2個, 商品C × 1個
   ```

3. **ログイン**
   ```
   ヘッダー → Login → ログインフォーム入力 → 送信
   ログイン成功 → トップページへリダイレクト
   ```

4. **カート確認**
   ```
   カートを開く
   期待結果:
   - 商品A × 3個（会員カート1個 + ゲストカート2個）
   - 商品B × 2個（会員カートのまま）
   - 商品C × 1個（ゲストカートから追加）
   ```

### 3. 上限超過時の制限

1. **会員カートに商品を追加**
   ```
   商品一覧 → 商品D を5個カートに追加
   ```

2. **ログアウト → ゲストでカートに商品を追加**
   ```
   商品一覧 → 商品D を7個カートに追加
   ```

3. **ログイン**
   ```
   ログイン → カート確認
   期待結果: 商品D × 9個（上限に制限される）
   コンソール: 警告メッセージが表示される
   ```

### 4. 在庫不足時のエラー

1. **管理画面で商品の在庫を0にする**
   ```
   管理画面 → 商品E の在庫を0に設定
   ```

2. **ゲストでカートに在庫0の商品を追加しようとする**
   ```
   商品一覧 → 商品E を追加
   期待結果: エラーメッセージが表示される
   ```

### 5. 冪等性の確認

1. **ブラウザの開発者ツールを開く**
   ```
   F12 → Network タブ
   ```

2. **会員登録またはログイン**
   ```
   ネットワークタブで /api/order/cart/merge のリクエストを確認
   ```

3. **ブラウザの戻るボタンで戻り、再度ログイン**
   ```
   期待結果: 2回目のmerge APIも成功レスポンス（エラーにならない）
   ```

---

## トラブルシューティング

### コンパイルエラー: UNIQUE制約の構文エラー

**原因**: JPA 2.0以降の構文を使用している

**解決策**: `pom.xml`でJakarta Persistence APIのバージョンを確認

### 実行時エラー: UNIQUE制約違反

**原因**: 既存データに重複がある

**解決策**:
```sql
-- 重複データを確認
SELECT session_id, product_id, type, COUNT(*)
FROM stock_reservations
WHERE type = 'TENTATIVE'
GROUP BY session_id, product_id, type
HAVING COUNT(*) > 1;

-- 重複データを削除（古いものを残す）
DELETE FROM stock_reservations
WHERE id NOT IN (
    SELECT MIN(id)
    FROM stock_reservations
    GROUP BY session_id, product_id, type
);
```

### フロントエンド: カート引き継ぎが実行されない

**原因**: `isMerging`フラグがtrueのままになっている

**解決策**: ブラウザをリロードするか、localStorageをクリア

### フロントエンド: 二重実行が発生する

**原因**: `isMerging`フラグが正しく機能していない

**解決策**: `mergeGuestCart`の`finally`ブロックで必ず`setIsMerging(false)`が実行されることを確認

---

## 注意事項

- **データベーススキーマの変更**: Task 1-2でエンティティを変更するため、アプリケーション再起動時にHibernateが自動でスキーマを更新します（`spring.jpa.hibernate.ddl-auto=update`の場合）
- **既存データへの影響**: 既存のゲストカートは`userId = null`のまま動作します
- **UNIQUE制約**: 既存データに重複がある場合、制約追加時にエラーが発生する可能性があります
- **トランザクション**: `@Transactional`が正しく付与されていることを確認してください
- **冪等性**: フロントエンドの二重実行ガードとバックエンドの自然な冪等性の両方が必要です
