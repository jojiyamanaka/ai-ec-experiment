# CHG-006 Task3: ゲスト共存とカート引き継ぎ（設計）

要件: `docs/01_requirements/CHG-006_Task3_ゲスト共存とカート引き継ぎ.md`
作成日: 2026-02-12
更新日: 2026-02-12（仮引当移行ロジックの改良）

---

## 1. 設計方針

ゲストカート（`sessionId`ベース）と会員カート（`userId`ベース）を共存させ、ログイン/登録時にゲストカートを会員カートへ引き継ぐ。

- **カート識別子の拡張**: `Cart`エンティティに`userId`フィールドを追加（nullable）
- **ゲストカート**: `userId = null`, `sessionId = "session-xxx"` で識別
- **会員カート**: `userId = 123`, `sessionId = "session-yyy"` で識別（会員カートにも固有のsessionIdを付与）
- **引き継ぎAPI**: `POST /api/order/cart/merge` を新規追加
- **引き継ぎタイミング**: ログイン/登録成功後、フロントエンドから引き継ぎAPIを呼び出す
- **仮引当の移行**: **UPDATEベースの移行**（解放→再引当ではない）
  - 会員カートに同じ商品がない → ゲストの仮引当をUPDATEで移行
  - 会員カートに同じ商品がある → 会員側の仮引当を更新、ゲスト側を解放
- **数量合算ルール**: 同一商品は数量を合算し、上限9個を超える場合は9個に制限
- **在庫整合性**: 引き継ぎ時も在庫チェックと仮引当の整合性を維持
- **データ整合性**: UNIQUE制約とトランザクション管理で二重予約を防止
- **冪等性**: 自然な冪等性（ゲストカート不在時は成功）+ フロントエンドガード

---

## 2. データモデル

### 2-1. Cart エンティティの拡張

**変更内容**: `userId`フィールドを追加

```java
@Entity
@Table(name = "carts")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    // 新規追加: 会員ID（ゲストカートの場合はnull）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ... 既存のメソッド
}
```

**データパターン**:

| ケース | `userId` | `sessionId` | 説明 |
|--------|----------|-------------|------|
| ゲストカート | `null` | `session-12345` | 未ログイン時のカート |
| 会員カート | `123` | `session-67890` | ログイン後のカート |

**注意**: 会員カートにも固有の`sessionId`を付与することで、仮引当（`StockReservation`）との紐付けを維持します。

### 2-2. StockReservation エンティティの拡張（UNIQUE制約追加）

**変更内容**: `(sessionId, productId, type)` の複合UNIQUE制約を追加

```java
@Entity
@Table(
    name = "stock_reservations",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_reservation_session_product_type",
        columnNames = {"session_id", "product_id", "type"}
    )
)
public class StockReservation {
    // ... 既存のフィールド
}
```

**マイグレーションSQL**:
```sql
ALTER TABLE stock_reservations
ADD CONSTRAINT uq_reservation_session_product_type
UNIQUE (session_id, product_id, type);
```

**目的**:
- ✓ 同一セッション・同一商品・同一種別の仮引当が複数作成されることを防止
- ✓ 仮引当移行時の衝突を検出可能にする
- ✓ 在庫計算の正確性を保証

### 2-3. CartRepository の拡張

**追加メソッド**:

```java
public interface CartRepository extends JpaRepository<Cart, Long> {
    // 既存
    Optional<Cart> findBySessionId(String sessionId);

    // 新規追加: 会員IDでカートを検索
    Optional<Cart> findByUserId(Long userId);
}
```

### 2-4. StockReservationRepository の拡張

**追加メソッド**:

```java
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    // 既存のメソッド...

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
}
```

---

## 3. API 設計

### 3-1. カート引き継ぎ（新規追加）

**エンドポイント**: `POST /api/order/cart/merge`

**ヘッダー**:
```
Authorization: Bearer <token>
```

**リクエスト**:
```json
{
  "guestSessionId": "session-12345"
}
```

**レスポンス（成功）**:
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "product": { /* Product */ },
        "quantity": 3
      }
    ],
    "totalQuantity": 3,
    "totalPrice": 3000
  },
  "warnings": [
    {
      "code": "QUANTITY_LIMITED",
      "message": "商品「オーガニックマンゴー」の数量を上限の9個に制限しました（元: 12個）"
    }
  ]
}
```

**エラーレスポンス**:
```json
// 在庫不足（一部商品のみ引き継ぎ）
{
  "success": false,
  "error": {
    "code": "PARTIAL_MERGE_FAILED",
    "message": "一部の商品を引き継げませんでした",
    "details": [
      {
        "productId": 2,
        "productName": "オーガニックアボカド",
        "requestedQuantity": 5,
        "availableStock": 0,
        "reason": "在庫不足"
      }
    ]
  }
}

// 認証なし
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "認証が必要です"
  }
}
```

**引き継ぎルール**:

1. **同一商品の数量合算**:
   - ゲストカート: 商品A × 3個
   - 会員カート: 商品A × 5個
   - 引き継ぎ後: 商品A × 8個

2. **上限超過時の制限**:
   - ゲストカート: 商品A × 7個
   - 会員カート: 商品A × 5個
   - 引き継ぎ後: 商品A × 9個（上限）
   - 警告メッセージを返す（`warnings`フィールド）

3. **在庫不足時**:
   - 引き継ぎ可能な商品のみ引き継ぎ
   - 在庫不足商品はエラー詳細（`details`）で通知
   - `success: false` だが、引き継ぎ成功分は`data`に含める（一部成功）

4. **ゲストカートが空の場合**:
   - エラーではなく、会員カートをそのまま返す
   - `success: true`（冪等性）

---

## 4. バックエンド実装

### 4-1. 変更: Cart エンティティ

**ファイル**: `backend/src/main/java/com/example/aiec/entity/Cart.java`

**追加フィールド**:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;
```

### 4-2. 変更: StockReservation エンティティ

**ファイル**: `backend/src/main/java/com/example/aiec/entity/StockReservation.java`

**UNIQUE制約の追加**:
```java
@Entity
@Table(
    name = "stock_reservations",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_reservation_session_product_type",
        columnNames = {"session_id", "product_id", "type"}
    )
)
public class StockReservation {
    // ... 既存のフィールド
}
```

### 4-3. 変更: CartRepository

**ファイル**: `backend/src/main/java/com/example/aiec/repository/CartRepository.java`

**追加メソッド**:
```java
Optional<Cart> findByUserId(Long userId);
```

### 4-4. 変更: StockReservationRepository

**ファイル**: `backend/src/main/java/com/example/aiec/repository/StockReservationRepository.java`

**追加メソッド**:
```java
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

### 4-5. 新規追加: DTO

**ファイル**: `backend/src/main/java/com/example/aiec/dto/MergeCartRequest.java`

```java
@Data
public class MergeCartRequest {
    @NotBlank(message = "ゲストセッションIDは必須です")
    private String guestSessionId;
}
```

**ファイル**: `backend/src/main/java/com/example/aiec/dto/MergeCartResponse.java`

```java
@Data
@AllArgsConstructor
public class MergeCartResponse {
    private CartDto cart;
    private List<MergeWarning> warnings;
    private List<MergeError> errors;

    @Data
    @AllArgsConstructor
    public static class MergeWarning {
        private String code;
        private String message;
    }

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

### 4-6. 変更: InventoryService

**ファイル**: `backend/src/main/java/com/example/aiec/service/InventoryService.java`

**新規メソッド**: 仮引当の移行（UPDATEベース）

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

### 4-7. 変更: CartService

**ファイル**: `backend/src/main/java/com/example/aiec/service/CartService.java`

**依存関係の追加**:
```java
private final UserRepository userRepository;
private final StockReservationRepository reservationRepository;
```

**新規メソッド**: カート引き継ぎ

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
        boolean quantityLimited = false;
        if (totalQuantity > MAX_QUANTITY_PER_ITEM) {
            totalQuantity = MAX_QUANTITY_PER_ITEM;
            quantityLimited = true;
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

### 4-8. 新規追加: Controller

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`（既存ファイルに追加）

**依存関係の追加**:
```java
private final AuthService authService;
```

**新規エンドポイント**:

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

private String extractToken(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new BusinessException("UNAUTHORIZED", "認証が必要です");
    }
    return authHeader.substring(7);
}
```

---

## 5. フロントエンド実装

### 5-1. 型定義の追加

**ファイル**: `frontend/src/types/api.ts`

```typescript
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

### 5-2. API関数の追加

**ファイル**: `frontend/src/lib/api.ts`

```typescript
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

### 5-3. AuthContext の変更

**ファイル**: `frontend/src/contexts/AuthContext.tsx`

**変更内容**: ログイン/登録成功後にカート引き継ぎを実行（二重実行ガード付き）

```typescript
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isMerging, setIsMerging] = useState(false) // 二重実行ガード

  // ... 既存のuseEffect

  const login = async (email: string, password: string) => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.login({ email, password })
      if (response.success && response.data) {
        const { user, token } = response.data
        setUser(user)
        setToken(token)
        localStorage.setItem('authToken', token)
        localStorage.setItem('authUser', JSON.stringify(user))

        // カート引き継ぎを実行
        await mergeGuestCart()
      } else {
        throw new Error(response.error?.message || 'ログインに失敗しました')
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'ログインエラー'
      setError(message)
      throw err
    } finally {
      setLoading(false)
    }
  }

  const register = async (email: string, displayName: string, password: string) => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.register({ email, displayName, password })
      if (response.success && response.data) {
        const { user, token } = response.data
        setUser(user)
        setToken(token)
        localStorage.setItem('authToken', token)
        localStorage.setItem('authUser', JSON.stringify(user))

        // カート引き継ぎを実行
        await mergeGuestCart()
      } else {
        throw new Error(response.error?.message || '登録に失敗しました')
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : '登録エラー'
      setError(message)
      throw err
    } finally {
      setLoading(false)
    }
  }

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

  // ... 既存のlogout, refreshUser, clearError

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!user,
        loading,
        error,
        register,
        login,
        logout,
        refreshUser,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
```

### 5-4. CartContext の変更

**ファイル**: `frontend/src/contexts/CartContext.tsx`

**変更内容**: カート引き継ぎ後にカート状態を更新

```typescript
export function CartProvider({ children }: { children: ReactNode }) {
  // ... 既存のstate

  // 既存のuseEffect...

  // カート引き継ぎイベントをリッスン
  useEffect(() => {
    const handleCartMerged = () => {
      refreshCart() // カート状態を再取得
    }
    window.addEventListener('cart:merged', handleCartMerged)
    return () => window.removeEventListener('cart:merged', handleCartMerged)
  }, [])

  // ... 既存のメソッド
}
```

---

## 6. データ整合性とトランザクション安全性

### 6-1. UNIQUE制約による二重予約防止

**制約**: `UNIQUE(session_id, product_id, type)`

**効果**:
- ✓ 同一セッション・同一商品・同一種別の仮引当が複数作成されることを防止
- ✓ バグやリトライで複数行ができることを防止
- ✓ 在庫計算の正確性を保証

**エッジケース対応**:
- UNIQUE制約違反時は例外がスローされる
- `transferReservation`ではTO側に既存予約があるかチェックし、衝突を回避

### 6-2. SELECT ... FOR UPDATE による並行制御

**実装**: `findAllActiveTentativeForUpdate`

**効果**:
- ✓ 仮引当の読み取り時に排他ロックを取得
- ✓ 同時に複数のトランザクションが同じ仮引当を操作することを防止
- ✓ 在庫計算の一貫性を保証

**動作**:
```
トランザクション1: SELECT ... FOR UPDATE → ロック取得
トランザクション2: SELECT ... FOR UPDATE → 待機
トランザクション1: UPDATE → コミット → ロック解放
トランザクション2: ロック取得 → 最新データで処理
```

### 6-3. トランザクション境界

**`@Transactional` の適用**:
- `CartService.mergeGuestCart`
- `InventoryService.transferReservation`
- `InventoryService.transferAllReservations`

**保証**:
- ✓ 全操作が成功するか、全て失敗するか（原子性）
- ✓ 途中で例外が発生した場合はロールバック
- ✓ コミット前は他のトランザクションから見えない（隔離性）

### 6-4. 冪等性の保証

**仕組み**:
1. **バックエンド**: ゲストカートが既に存在しない場合は成功レスポンス
2. **フロントエンド**: 二重実行ガード（`isMerging`フラグ）

**シナリオ**:
- 1回目: ゲストカートを引き継ぎ、削除
- 2回目: ゲストカートが存在しない → 会員カートをそのまま返す（成功）
- 結果: どちらも成功レスポンス（冪等）

---

## 7. 処理フロー

### 7-1. ログイン時のカート引き継ぎフロー

```
ユーザー → ログインフォーム送信
  → AuthContext.login()
    → POST /api/auth/login
      → 成功: トークンとユーザー情報を取得
    → mergeGuestCart() (isMerging=falseの場合のみ)
      → isMerging = true
      → POST /api/order/cart/merge
        → リクエスト: { guestSessionId: "session-12345" }
        → バックエンド（@Transactional）:
          1. ゲストカートを取得（sessionId）
          2. 会員カートを取得または作成（userId）
          3-a. 会員カートなし → ゲストカートを昇格（userIdを設定）
          3-b. 会員カートあり → 商品をマージ:
             - 商品ごとにループ:
               - 会員カートに同じ商品なし → transferReservation（UPDATE移行）
               - 会員カートに同じ商品あり → updateReservation（会員側）+ releaseReservation（ゲスト側）
          4. ゲストカートの残存仮引当を全解放
          5. ゲストカートを削除
        → レスポンス: 引き継ぎ後のカート + 警告 + エラー
      → isMerging = false
      → CartContext: cart:merged イベントで状態を更新
  → ログイン成功 → トップページへリダイレクト
```

### 7-2. 仮引当移行の詳細フロー（UPDATEベース）

```
InventoryService.transferReservation(fromSessionId, toSessionId, productId)
  → 1. FROM側の予約を取得（SELECT ... FOR UPDATE）
    → 排他ロック取得（他のトランザクションは待機）
    → 複数行ある場合は全件取得、合計数量を計算
  → 2. TO側の予約を取得（SELECT ... FOR UPDATE）
    → 排他ロック取得
  → 3-a. TO側に予約あり:
    → TO側の数量を更新（UPDATE）: quantity += FROM側の合計数量
    → FROM側を全削除（DELETE）
  → 3-b. TO側に予約なし:
    → FROM側の1件目をUPDATE: sessionId = toSessionId, quantity = 合計数量
    → FROM側の2件目以降を削除（DELETE）（通常はないが安全のため）
  → 4. コミット → ロック解放
```

**重要な特性**:
- ✓ UPDATEベースなので在庫の一時的な変動なし
- ✓ SELECT ... FOR UPDATEで並行制御
- ✓ UNIQUE制約で衝突を検出
- ✓ 複数行（バグやリトライ）でも全件処理

---

## 8. 既存パターンとの整合性

| 観点 | 既存パターン | CHG-006 Task3 |
|------|-------------|---------------|
| エンティティ | Cart（sessionIdのみ） | Cart（sessionId + userId） |
| Repository | findBySessionId | findBySessionId + findByUserId |
| カート識別 | sessionIdのみ | sessionId（必須）+ userId（オプション） |
| 仮引当 | sessionIdベース | 同様（会員カートもsessionIdで仮引当） |
| DTO変換 | fromEntity() | 同様 |
| バリデーション | @Valid | 同様 |
| 例外処理 | BusinessException, ConflictException | 同様 + 一部成功のレスポンス |
| レスポンス形式 | ApiResponse | 同様 + warnings, errors フィールド |
| トランザクション | @Transactional | 同様 + SELECT ... FOR UPDATE |

---

## 9. セキュリティ考慮事項

### 9-1. 認証
- カート引き継ぎAPIは認証必須（`Authorization: Bearer <token>`）
- トークンからユーザーIDを取得し、他のユーザーのカートを操作できないことを保証

### 9-2. セッションハイジャック対策
- ゲストセッションIDはフロントエンドのlocalStorageから取得
- セッションIDの推測は困難（UUID形式）

### 9-3. 在庫整合性
- 引き継ぎ時も在庫チェックと仮引当を厳密に管理
- トランザクションとUNIQUE制約で整合性を保証

---

## 10. テスト観点

### 10-1. 引き継ぎ成功パターン

- [ ] **ケース1: 会員カートが存在しない**
  - ゲストカート: 商品A × 3個
  - 引き継ぎ後: 商品A × 3個（ゲストカートを昇格）
  - 仮引当: そのまま維持（移行不要）

- [ ] **ケース2: 会員カートが空**
  - ゲストカート: 商品A × 3個
  - 会員カート: 空
  - 引き継ぎ後: 商品A × 3個
  - 仮引当: UPDATE移行

- [ ] **ケース3: 異なる商品**
  - ゲストカート: 商品A × 3個
  - 会員カート: 商品B × 2個
  - 引き継ぎ後: 商品A × 3個, 商品B × 2個
  - 仮引当: 商品AはUPDATE移行、商品Bはそのまま

- [ ] **ケース4: 同一商品の合算**
  - ゲストカート: 商品A × 3個
  - 会員カート: 商品A × 2個
  - 引き継ぎ後: 商品A × 5個
  - 仮引当: 会員側をUPDATE（5個）、ゲスト側を削除

### 10-2. 上限超過パターン

- [ ] **ケース5: 上限9個に制限**
  - ゲストカート: 商品A × 7個
  - 会員カート: 商品A × 5個
  - 引き継ぎ後: 商品A × 9個
  - 警告: 「商品Aの数量を上限の9個に制限しました」

### 10-3. 在庫不足パターン

- [ ] **ケース6: 在庫不足（一部商品のみ引き継ぎ）**
  - ゲストカート: 商品A × 3個（在庫: 5個）, 商品B × 5個（在庫: 0個）
  - 引き継ぎ後: 商品A × 3個のみ
  - エラー: 「商品Bは在庫不足のため引き継げませんでした」

### 10-4. データ整合性パターン

- [ ] **ケース7: UNIQUE制約の動作確認**
  - 同一(sessionId, productId, type)の仮引当を2回作成しようとする
  - UNIQUE制約違反エラーが発生すること

- [ ] **ケース8: 複数行の仮引当（バグシミュレーション）**
  - 手動でゲストカートに同一商品の仮引当を2件作成
  - transferReservationが全件処理すること（合計数量で移行）

- [ ] **ケース9: SELECT ... FOR UPDATEの並行制御**
  - 2つのトランザクションが同時に同じ仮引当を移行しようとする
  - 一方が待機し、順次処理されること

### 10-5. 冪等性パターン

- [ ] **ケース10: 2回目の引き継ぎ実行**
  - 1回目: 正常に引き継ぎ
  - 2回目: ゲストカートが存在しない → 成功レスポンス（エラーではない）

- [ ] **ケース11: フロントエンドの二重実行ガード**
  - ダブルクリックでmergeGuestCartを2回呼び出す
  - 2回目は実行されない（isMergingフラグでガード）

### 10-6. 後方互換性

- [ ] **ケース12: 未ログイン時の既存機能**
  - 商品閲覧、カート追加、注文が従来どおり動作すること
  - ゲストカート（`userId = null`）が正常に機能すること

---

## 11. 今後の拡張（対象外）

本タスク（Task3）では実装しない機能:

- **会員注文履歴**: ログインユーザーの過去の注文一覧表示（Task4）
- **管理者ロール制御**: 管理者権限の管理（Task5）
- **複数デバイス間のカート同期**: 同一会員が複数デバイスでログインした際のカート統合

---

## 12. エラーメッセージ一覧

| エラーコード | HTTPステータス | メッセージ | 発生条件 |
|------------|--------------|----------|----------|
| `UNAUTHORIZED` | 401 | 認証が必要です | トークンなし・無効 |
| `USER_NOT_FOUND` | 404 | ユーザーが見つかりません | 会員IDが無効 |
| `CART_NOT_FOUND` | 404 | カートが見つかりません | セッションIDが無効 |
| `PARTIAL_MERGE_FAILED` | 400 | 一部の商品を引き継げませんでした | 在庫不足・非公開商品あり |
| `INSUFFICIENT_STOCK` | 409 | 在庫が不足しています | 引き継ぎ時の在庫不足 |
| `ITEM_NOT_AVAILABLE` | 400 | この商品は現在購入できません | 非公開商品 |

---

## 13. データ移行

既存のカートデータは`userId`が`null`のままですが、新しいロジックはそのまま動作します。

**マイグレーション**:
1. `users`テーブルが既に存在すること（Task1で作成済み）
2. `carts`テーブルに`user_id`カラムを追加（nullable）
3. `stock_reservations`テーブルにUNIQUE制約を追加

**既存データへの影響**:
- 既存のゲストカートは`userId = null`として動作
- ログイン時に引き継ぎが実行され、`userId`が設定される
- UNIQUE制約は既存データに影響しない（過去のデータは許容）
