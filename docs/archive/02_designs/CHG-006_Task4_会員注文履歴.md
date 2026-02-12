# CHG-006 Task4: 会員注文履歴（設計）

要件: `docs/01_requirements/CHG-006_Task4_会員注文履歴.md`
作成日: 2026-02-12

---

## 1. 設計方針

注文を会員単位で管理し、会員が自分の注文履歴を参照できるようにする。

- **注文識別子の拡張**: `Order`エンティティに`userId`フィールドを追加（nullable）
- **ゲスト注文**: `userId = null`, `sessionId = "session-xxx"` で識別（既存の仕組みを維持）
- **会員注文**: `userId = 123`, `sessionId = "session-yyy"` で識別
- **注文履歴API**: `GET /api/order/history` を新規追加（認証必須）
- **権限チェック**: 注文詳細取得時に会員注文とゲスト注文を区別して権限チェック
- **後方互換性**: 既存のゲスト注文フローを維持
- **退会時方針**: 注文データを保持し、`userId`をnullにして匿名化（詳細は本タスクで定義、実装は対象外）

---

## 2. データモデル

### 2-1. Order エンティティの拡張

**変更内容**: `userId`フィールドを追加

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private String sessionId;

    // 新規追加: 会員ID（ゲスト注文の場合はnull）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

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
| ゲスト注文 | `null` | `session-12345` | 未ログイン時の注文 |
| 会員注文 | `123` | `session-67890` | ログイン時の注文 |

**注意**: 会員注文にも`sessionId`は保持されます（カートとの整合性のため）。

### 2-2. OrderRepository の拡張

**追加メソッド**:

```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 既存
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    // 新規追加: 会員IDで注文一覧を取得（作成日時降順）
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}
```

---

## 3. API 設計

### 3-1. 注文履歴一覧（新規追加）

**エンドポイント**: `GET /api/order/history`

**ヘッダー**:
```
Authorization: Bearer <token>
```

**レスポンス（成功）**:
```json
{
  "success": true,
  "data": [
    {
      "orderId": 3,
      "orderNumber": "ORD-20260212-003",
      "items": [
        {
          "product": {
            "id": 1,
            "name": "オーガニックマンゴー",
            "price": 1000,
            "image": "/images/mango.jpg"
          },
          "quantity": 2,
          "subtotal": 2000
        }
      ],
      "totalPrice": 2000,
      "status": "DELIVERED",
      "createdAt": "2026-02-10T10:00:00Z",
      "updatedAt": "2026-02-11T15:00:00Z"
    },
    {
      "orderId": 1,
      "orderNumber": "ORD-20260208-001",
      "items": [ /* ... */ ],
      "totalPrice": 3000,
      "status": "SHIPPED",
      "createdAt": "2026-02-08T09:00:00Z",
      "updatedAt": "2026-02-09T14:00:00Z"
    }
  ]
}
```

**エラーレスポンス**:
```json
// 認証なし（401 Unauthorized）
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "認証が必要です"
  }
}
```

**仕様**:
- 認証必須
- 自分の注文のみ取得（`userId`で絞り込み）
- 作成日時降順でソート
- 全ステータスの注文を取得

### 3-2. 注文詳細取得（既存APIの拡張）

**エンドポイント**: `GET /api/order/{id}`

**ヘッダー**:
```
Authorization: Bearer <token> (オプション、会員注文の場合は必須)
X-Session-Id: <sessionId> (オプション、ゲスト注文の場合は必須)
```

**権限チェックロジック**:
```
注文がユーザーに紐づいている場合（order.userId != null）:
  - AuthorizationヘッダーからuserIdを取得
  - order.userId == currentUserId なら許可
  - それ以外は 404 (ORDER_NOT_FOUND)

注文がゲストの場合（order.userId == null）:
  - X-Session-IdヘッダーからsessionIdを取得
  - order.sessionId == currentSessionId なら許可
  - それ以外は 404 (ORDER_NOT_FOUND)
```

**レスポンス**: 既存と同じ

### 3-3. 注文作成（既存APIの拡張）

**エンドポイント**: `POST /api/order`

**ヘッダー**:
```
Authorization: Bearer <token> (オプション、ログイン時のみ)
X-Session-Id: <sessionId> (必須)
```

**リクエスト**: 既存と同じ

**変更内容**:
- Authorizationヘッダーがある場合、注文に`userId`を設定
- Authorizationヘッダーがない場合、従来どおりゲスト注文として作成

**レスポンス**: 既存と同じ

---

## 4. バックエンド実装

### 4-1. 変更: Order エンティティ

**ファイル**: `backend/src/main/java/com/example/aiec/entity/Order.java`

**追加フィールド**:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;
```

**import追加**:
```java
import com.example.aiec.entity.User;
```

### 4-2. 変更: OrderRepository

**ファイル**: `backend/src/main/java/com/example/aiec/repository/OrderRepository.java`

**追加メソッド**:
```java
/**
 * 会員IDで注文一覧を取得（作成日時降順）
 */
List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
```

### 4-3. 変更: OrderService

**ファイル**: `backend/src/main/java/com/example/aiec/service/OrderService.java`

**依存関係の追加**:
```java
private final UserRepository userRepository;
```

**新規メソッド**: 注文履歴一覧取得

```java
/**
 * 会員の注文履歴を取得
 *
 * @param userId 会員ID（認証済み）
 * @return 注文履歴一覧（作成日時降順）
 */
@Transactional(readOnly = true)
public List<OrderDto> getOrderHistory(Long userId) {
    return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(OrderDto::fromEntity)
            .toList();
}
```

**変更メソッド**: 注文作成

```java
/**
 * 注文を作成
 *
 * @param sessionId セッションID
 * @param cartId カートID
 * @param userId 会員ID（オプション、ログイン時のみ）
 * @return 作成された注文
 */
@Transactional
public OrderDto createOrder(String sessionId, String cartId, Long userId) {
    // セッションIDとカートIDが一致するか確認
    if (!sessionId.equals(cartId)) {
        throw new BusinessException("INVALID_REQUEST", "無効なリクエストです");
    }

    Cart cart = cartRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("CART_NOT_FOUND", "カートが見つかりません"));

    // カートが空でないかチェック
    if (cart.getItems().isEmpty()) {
        throw new BusinessException("CART_EMPTY", "カートが空です");
    }

    // 非公開商品チェック（既存のロジック）
    List<UnavailableProductDetail> unavailableProducts = cart.getItems().stream()
            .filter(item -> !item.getProduct().getIsPublished())
            .map(item -> new UnavailableProductDetail(
                    item.getProduct().getId(),
                    item.getProduct().getName()))
            .toList();

    if (!unavailableProducts.isEmpty()) {
        throw new ItemNotAvailableException(
                "ITEM_NOT_AVAILABLE",
                "購入できない商品がカートに含まれています",
                unavailableProducts);
    }

    // 注文を作成
    Order order = new Order();
    order.setOrderNumber(generateOrderNumber());
    order.setSessionId(sessionId);
    order.setTotalPrice(cart.getTotalPrice());
    order.setStatus(Order.OrderStatus.PENDING);

    // 会員IDを設定（ログイン時のみ）
    if (userId != null) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "ユーザーが見つかりません"));
        order.setUser(user);
    }

    // カートアイテムを注文アイテムに変換（既存のロジック）
    cart.getItems().forEach(cartItem -> {
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(cartItem.getProduct());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setSubtotal(cartItem.getProduct().getPrice() * cartItem.getQuantity());
        order.addItem(orderItem);
    });

    Order savedOrder = orderRepository.save(order);

    // 仮引当 → 本引当に変換（stock 減少込み）
    inventoryService.commitReservations(sessionId, savedOrder);

    // カートをクリア（仮引当の解除はスキップ、既に本引当済み）
    cartRepository.findBySessionId(sessionId)
            .ifPresent(c -> {
                c.getItems().clear();
                cartRepository.save(c);
            });

    return OrderDto.fromEntity(savedOrder);
}
```

**変更メソッド**: 注文詳細取得（権限チェック拡張）

```java
/**
 * 注文詳細を取得
 *
 * @param id 注文ID
 * @param sessionId セッションID（ゲスト注文の場合）
 * @param userId 会員ID（会員注文の場合）
 * @return 注文詳細
 */
@Transactional(readOnly = true)
public OrderDto getOrderById(Long id, String sessionId, Long userId) {
    Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

    // 権限チェック
    boolean canAccess = false;

    if (order.getUser() != null) {
        // 会員注文: userIdが一致すること
        canAccess = userId != null && order.getUser().getId().equals(userId);
    } else {
        // ゲスト注文: sessionIdが一致すること
        canAccess = sessionId != null && order.getSessionId().equals(sessionId);
    }

    if (!canAccess) {
        throw new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません");
    }

    return OrderDto.fromEntity(order);
}
```

**変更メソッド**: 注文キャンセル（権限チェック拡張）

```java
/**
 * 注文キャンセル
 *
 * @param orderId 注文ID
 * @param sessionId セッションID（ゲスト注文の場合）
 * @param userId 会員ID（会員注文の場合）
 * @return キャンセルされた注文
 */
@Transactional
public OrderDto cancelOrder(Long orderId, String sessionId, Long userId) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

    // 権限チェック
    boolean canAccess = false;

    if (order.getUser() != null) {
        // 会員注文: userIdが一致すること
        canAccess = userId != null && order.getUser().getId().equals(userId);
    } else {
        // ゲスト注文: sessionIdが一致すること
        canAccess = sessionId != null && order.getSessionId().equals(sessionId);
    }

    if (!canAccess) {
        throw new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません");
    }

    // 本引当を解除（stock 戻し + ステータス変更込み）
    inventoryService.releaseCommittedReservations(orderId);

    // 注文を再取得（InventoryServiceで更新されたため）
    order = orderRepository.findById(orderId).orElseThrow();
    return OrderDto.fromEntity(order);
}
```

### 4-4. 変更: OrderController

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**依存関係の追加**:
```java
private final AuthService authService;
```

**新規エンドポイント**: 注文履歴一覧

```java
/**
 * 会員の注文履歴を取得
 */
@GetMapping("/history")
public ApiResponse<List<OrderDto>> getOrderHistory(
        @RequestHeader("Authorization") String authHeader) {

    // トークンからユーザーIDを取得
    String token = extractToken(authHeader);
    User user = authService.verifyToken(token);

    List<OrderDto> orders = orderService.getOrderHistory(user.getId());
    return ApiResponse.success(orders);
}
```

**変更エンドポイント**: 注文作成

```java
/**
 * 注文作成
 */
@PostMapping
public ApiResponse<OrderDto> createOrder(
        @RequestHeader("X-Session-Id") String sessionId,
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @Valid @RequestBody CreateOrderRequest request) {

    // 認証済みの場合はuserIdを取得
    Long userId = null;
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        try {
            String token = extractToken(authHeader);
            User user = authService.verifyToken(token);
            userId = user.getId();
        } catch (Exception e) {
            // 認証エラーの場合はゲスト注文として処理
            // （トークンが無効でもゲスト注文は許可）
        }
    }

    OrderDto order = orderService.createOrder(sessionId, request.getCartId(), userId);
    return ApiResponse.success(order);
}
```

**変更エンドポイント**: 注文詳細取得

```java
/**
 * 注文詳細を取得
 */
@GetMapping("/{id}")
public ApiResponse<OrderDto> getOrderById(
        @PathVariable Long id,
        @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    // 認証済みの場合はuserIdを取得
    Long userId = null;
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        try {
            String token = extractToken(authHeader);
            User user = authService.verifyToken(token);
            userId = user.getId();
        } catch (Exception e) {
            // 認証エラーの場合はnullのまま
        }
    }

    OrderDto order = orderService.getOrderById(id, sessionId, userId);
    return ApiResponse.success(order);
}
```

**変更エンドポイント**: 注文キャンセル

```java
/**
 * 注文をキャンセル
 */
@PostMapping("/{id}/cancel")
public ApiResponse<OrderDto> cancelOrder(
        @PathVariable Long id,
        @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    // 認証済みの場合はuserIdを取得
    Long userId = null;
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        try {
            String token = extractToken(authHeader);
            User user = authService.verifyToken(token);
            userId = user.getId();
        } catch (Exception e) {
            // 認証エラーの場合はnullのまま
        }
    }

    OrderDto order = orderService.cancelOrder(id, sessionId, userId);
    return ApiResponse.success(order);
}
```

**ヘルパーメソッド**:

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
```

---

## 5. フロントエンド実装

### 5-1. 型定義の追加

**ファイル**: `frontend/src/types/api.ts`

既存の`Order`型はそのまま使用（変更不要）

### 5-2. API関数の追加

**ファイル**: `frontend/src/lib/api.ts`

```typescript
/**
 * 会員の注文履歴を取得
 */
export async function getOrderHistory(): Promise<ApiResponse<Order[]>> {
  return fetchApi<Order[]>('/api/order/history')
}
```

**注意**: `fetchApi`は既にAuthorizationヘッダーを自動付与するため、追加の変更は不要。

### 5-3. 注文履歴ページの作成

**ファイル**: `frontend/src/pages/OrderHistoryPage.tsx`（新規作成）

```typescript
import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../contexts/AuthContext'
import * as api from '../lib/api'
import type { Order } from '../types/api'

export default function OrderHistoryPage() {
  const { isAuthenticated } = useAuth()
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!isAuthenticated) {
      return
    }

    const fetchOrders = async () => {
      setLoading(true)
      setError(null)
      try {
        const response = await api.getOrderHistory()
        if (response.success && response.data) {
          setOrders(response.data)
        } else {
          setError(response.error?.message || '注文履歴の取得に失敗しました')
        }
      } catch (err) {
        setError('注文履歴の取得中にエラーが発生しました')
        console.error(err)
      } finally {
        setLoading(false)
      }
    }

    fetchOrders()
  }, [isAuthenticated])

  if (!isAuthenticated) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-12">
        <p className="text-center text-zinc-600">
          注文履歴を表示するには
          <Link to="/auth/login" className="ml-2 text-zinc-900 underline">
            ログイン
          </Link>
          してください
        </p>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-12">
        <p className="text-center text-zinc-600">読み込み中...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-12">
        <p className="text-center text-red-600">{error}</p>
      </div>
    )
  }

  if (orders.length === 0) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-12">
        <h1 className="mb-8 text-center font-serif text-3xl tracking-wider">
          注文履歴
        </h1>
        <p className="text-center text-zinc-600">注文履歴がありません</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl px-6 py-12">
      <h1 className="mb-8 text-center font-serif text-3xl tracking-wider">
        注文履歴
      </h1>

      <div className="space-y-6">
        {orders.map((order) => (
          <div
            key={order.orderId}
            className="rounded border border-zinc-200 bg-white p-6"
          >
            <div className="mb-4 flex items-center justify-between">
              <div>
                <p className="text-sm text-zinc-500">注文番号</p>
                <p className="font-mono text-lg">{order.orderNumber}</p>
              </div>
              <div>
                <span
                  className={`rounded px-3 py-1 text-sm ${
                    order.status === 'DELIVERED'
                      ? 'bg-green-100 text-green-800'
                      : order.status === 'SHIPPED'
                      ? 'bg-blue-100 text-blue-800'
                      : order.status === 'CANCELLED'
                      ? 'bg-red-100 text-red-800'
                      : 'bg-zinc-100 text-zinc-800'
                  }`}
                >
                  {order.status}
                </span>
              </div>
            </div>

            <div className="mb-4 space-y-2">
              {order.items.map((item, index) => (
                <div key={index} className="flex items-center space-x-4">
                  <img
                    src={item.product.image}
                    alt={item.product.name}
                    className="h-16 w-16 object-cover rounded"
                  />
                  <div className="flex-1">
                    <p className="font-medium">{item.product.name}</p>
                    <p className="text-sm text-zinc-600">
                      数量: {item.quantity} × ¥{item.product.price.toLocaleString()}
                    </p>
                  </div>
                  <p className="font-medium">
                    ¥{item.subtotal.toLocaleString()}
                  </p>
                </div>
              ))}
            </div>

            <div className="flex items-center justify-between border-t border-zinc-200 pt-4">
              <div>
                <p className="text-sm text-zinc-500">注文日時</p>
                <p className="text-sm">
                  {new Date(order.createdAt).toLocaleDateString('ja-JP')}
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm text-zinc-500">合計金額</p>
                <p className="text-xl font-bold">
                  ¥{order.totalPrice.toLocaleString()}
                </p>
              </div>
            </div>

            <div className="mt-4">
              <Link
                to={`/order/${order.orderId}`}
                className="block w-full rounded bg-zinc-900 px-4 py-2 text-center text-sm uppercase tracking-widest text-white hover:bg-zinc-800"
              >
                詳細を見る
              </Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
```

### 5-4. ルーティングの追加

**ファイル**: `frontend/src/App.tsx`

**import追加**:
```typescript
import OrderHistoryPage from './pages/OrderHistoryPage'
```

**ルート追加**:
```typescript
<Route path="/order/history" element={<OrderHistoryPage />} />
```

### 5-5. Layoutの変更（注文履歴リンクを追加）

**ファイル**: `frontend/src/components/Layout.tsx`

**変更内容**: ヘッダーの認証状態表示部分に注文履歴リンクを追加

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

---

## 6. 処理フロー

### 6-1. 会員注文の作成フロー

```
ユーザー（ログイン済み） → 注文確認ページで「注文する」ボタンをクリック
  → POST /api/order
    → ヘッダー: Authorization: Bearer <token>, X-Session-Id: <sessionId>
    → OrderController:
      1. AuthorizationヘッダーからuserIdを取得
      2. OrderService.createOrder(sessionId, cartId, userId)
    → OrderService:
      1. カートを取得
      2. 注文を作成
      3. userIdをOrderに設定（会員注文）
      4. 注文アイテムを作成
      5. 仮引当 → 本引当に変換
      6. カートをクリア
    → レスポンス: 作成された注文
  → 注文完了ページへリダイレクト
```

### 6-2. ゲスト注文の作成フロー（既存）

```
ユーザー（未ログイン） → 注文確認ページで「注文する」ボタンをクリック
  → POST /api/order
    → ヘッダー: X-Session-Id: <sessionId>（Authorizationなし）
    → OrderController:
      1. Authorizationヘッダーがない → userId = null
      2. OrderService.createOrder(sessionId, cartId, null)
    → OrderService:
      1. カートを取得
      2. 注文を作成
      3. userIdはnull（ゲスト注文）
      4. 注文アイテムを作成
      5. 仮引当 → 本引当に変換
      6. カートをクリア
    → レスポンス: 作成された注文
  → 注文完了ページへリダイレクト
```

### 6-3. 注文履歴一覧取得フロー

```
ユーザー（ログイン済み） → ヘッダーの「Orders」リンクをクリック
  → /order/history ページへ遷移
  → useEffect:
    → GET /api/order/history
      → ヘッダー: Authorization: Bearer <token>
      → OrderController.getOrderHistory()
        1. トークンからuserIdを取得
        2. OrderService.getOrderHistory(userId)
          → OrderRepository.findByUserIdOrderByCreatedAtDesc(userId)
          → 作成日時降順で注文一覧を返す
      → レスポンス: 注文一覧
    → 注文一覧を表示
```

### 6-4. 注文詳細取得フロー（会員注文）

```
ユーザー（ログイン済み） → 注文履歴から「詳細を見る」をクリック
  → GET /api/order/{id}
    → ヘッダー: Authorization: Bearer <token>
    → OrderController.getOrderById(id, sessionId, authHeader)
      1. トークンからuserIdを取得
      2. OrderService.getOrderById(id, null, userId)
    → OrderService:
      1. 注文を取得
      2. 権限チェック: order.userId == userId
      3. 一致すれば注文を返す、不一致なら404
    → レスポンス: 注文詳細
  → 注文詳細を表示
```

### 6-5. 注文詳細取得フロー（ゲスト注文）

```
ユーザー（未ログイン） → 注文完了ページから「詳細を見る」をクリック
  → GET /api/order/{id}
    → ヘッダー: X-Session-Id: <sessionId>
    → OrderController.getOrderById(id, sessionId, null)
      1. userIdはnull
      2. OrderService.getOrderById(id, sessionId, null)
    → OrderService:
      1. 注文を取得
      2. 権限チェック: order.sessionId == sessionId
      3. 一致すれば注文を返す、不一致なら404
    → レスポンス: 注文詳細
  → 注文詳細を表示
```

---

## 7. 既存パターンとの整合性

| 観点 | 既存パターン | CHG-006 Task4 |
|------|-------------|---------------|
| エンティティ | Order（sessionIdのみ） | Order（sessionId + userId） |
| Repository | findBySessionIdOrderByCreatedAtDesc | 同様 + findByUserIdOrderByCreatedAtDesc |
| 注文識別 | sessionIdのみ | sessionId（必須）+ userId（オプション） |
| 権限チェック | sessionId一致 | sessionIdまたはuserId一致 |
| DTO変換 | fromEntity() | 同様 |
| 例外処理 | ResourceNotFoundException | 同様 |
| レスポンス形式 | ApiResponse | 同様 |

---

## 8. セキュリティ考慮事項

### 8-1. 権限チェック
- **会員注文**: `order.userId == currentUserId` で自分の注文のみアクセス可能
- **ゲスト注文**: `order.sessionId == currentSessionId` で自分の注文のみアクセス可能
- 他会員の注文は403ではなく404を返す（注文の存在を隠蔽）

### 8-2. 認証トークンの検証
- 注文履歴APIは認証必須
- 注文作成・詳細取得・キャンセルAPIは認証オプション（ゲスト注文も許可）

### 8-3. セッションハイジャック対策
- セッションIDはフロントエンドのlocalStorageから取得
- セッションIDの推測は困難（UUID形式）

---

## 9. 退会時方針（REQ-4-004）

### 9-1. 方針: 注文データの匿名化

**選択肢の比較**:

| 方針 | メリット | デメリット |
|------|---------|-----------|
| **A: 匿名化**（推奨） | 注文履歴を保持、監査可能 | 完全削除ではない |
| B: 論理削除 | 復元可能 | 管理が複雑 |
| C: 物理削除 | データが残らない | 監査不可、トラブル時に対処困難 |

**採用方針**: **A（匿名化）**

**理由**:
- 注文データは取引履歴として保持が必要
- 監査・統計分析に活用
- トラブル時の調査が可能

### 9-2. 実装方針（本タスクでは対象外、Task5以降で実装）

**ユーザー削除時の処理**:

```java
@Transactional
public void deleteUser(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "ユーザーが見つかりません"));

    // 注文を匿名化（userIdをnullにする）
    List<Order> userOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    for (Order order : userOrders) {
        order.setUser(null); // 匿名化
    }
    orderRepository.saveAll(userOrders);

    // カートを匿名化（userIdをnullにする）
    Cart userCart = cartRepository.findByUserId(userId).orElse(null);
    if (userCart != null) {
        userCart.setUser(null); // 匿名化
        cartRepository.save(userCart);
    }

    // ユーザーを削除
    userRepository.delete(user);
}
```

**匿名化後の注文**:
- `userId = null`（ゲスト注文と同じ状態）
- `sessionId`は保持される
- 退会後のユーザーは注文履歴にアクセスできない
- 管理者は注文履歴を参照可能（監査・統計用）

---

## 10. テスト観点

### 10-1. 注文紐付け

- [ ] **ケース1: ログイン時の注文作成**
  - ログイン状態で注文作成
  - 注文に`userId`が設定される

- [ ] **ケース2: 未ログイン時の注文作成**
  - 未ログイン状態で注文作成
  - 注文の`userId`がnull

### 10-2. 注文履歴一覧

- [ ] **ケース3: 会員の注文履歴取得**
  - 会員が3件の注文を作成
  - 注文履歴APIで3件取得できる
  - 作成日時降順でソートされている

- [ ] **ケース4: 他会員の注文が含まれない**
  - 会員Aと会員Bがそれぞれ注文を作成
  - 会員Aの注文履歴APIで会員Aの注文のみ取得

- [ ] **ケース5: 未ログイン時の注文履歴取得**
  - 認証なしで注文履歴APIを呼び出す
  - 401エラーが返される

### 10-3. 注文詳細保護

- [ ] **ケース6: 会員が自分の注文を取得**
  - 会員Aが自分の注文詳細を取得
  - 正常に取得できる

- [ ] **ケース7: 会員が他会員の注文を取得**
  - 会員Aが会員Bの注文詳細を取得しようとする
  - 404エラーが返される

- [ ] **ケース8: ゲストが自分の注文を取得**
  - ゲストが自分のsessionIdの注文詳細を取得
  - 正常に取得できる

- [ ] **ケース9: ゲストが他のゲストの注文を取得**
  - ゲストAがゲストBの注文詳細を取得しようとする
  - 404エラーが返される

### 10-4. 後方互換性

- [ ] **ケース10: 既存のゲスト注文フローが動作**
  - 未ログイン時に注文作成・詳細取得・キャンセルができる

- [ ] **ケース11: 既存の注文詳細画面が動作**
  - 注文完了ページから注文詳細ページへ遷移できる

---

## 11. 今後の拡張（対象外）

本タスク（Task4）では実装しない機能:

- **退会機能の実装**: ユーザー削除と注文匿名化の実装（方針のみ定義）
- **管理者ロール制御**: 管理者が全注文を参照できる機能（Task5）
- **注文フィルタリング**: ステータスや期間でフィルタリング
- **ページネーション**: 注文履歴が多い場合のページング
- **注文検索**: 注文番号や商品名で検索

---

## 12. エラーメッセージ一覧

| エラーコード | HTTPステータス | メッセージ | 発生条件 |
|------------|--------------|----------|----------|
| `UNAUTHORIZED` | 401 | 認証が必要です | トークンなし・無効（注文履歴API） |
| `ORDER_NOT_FOUND` | 404 | 注文が見つかりません | 注文が存在しない、または権限なし |
| `USER_NOT_FOUND` | 404 | ユーザーが見つかりません | 会員IDが無効 |

---

## 13. データ移行

既存の注文データは`userId`が`null`のままですが、新しいロジックはそのまま動作します。

**マイグレーション**:
1. `users`テーブルが既に存在すること（Task1で作成済み）
2. `orders`テーブルに`user_id`カラムを追加（nullable）

**既存データへの影響**:
- 既存のゲスト注文は`userId = null`として動作
- 既存のゲスト注文は会員の注文履歴に表示されない
- ログイン後の新規注文から`userId`が設定される
