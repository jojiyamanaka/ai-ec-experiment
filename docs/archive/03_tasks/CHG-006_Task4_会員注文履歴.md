# CHG-006 Task4: 会員注文履歴（実装タスク）

要件: `docs/01_requirements/CHG-006_Task4_会員注文履歴.md`
設計: `docs/02_designs/CHG-006_Task4_会員注文履歴.md`
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

# 動作確認（会員登録）
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "displayName": "Test User"
  }'

# 動作確認（ログイン）
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
# レスポンスからtokenを取得（以降の例では<TOKEN>として参照）

# 動作確認（注文作成 - 会員）
curl -X POST http://localhost:8080/api/order \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: session-test-001" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "cartId": "session-test-001"
  }'

# 動作確認（注文履歴取得）
curl -X GET http://localhost:8080/api/order/history \
  -H "Authorization: Bearer <TOKEN>"
```

### フロントエンド検証
```bash
# フロントエンドコンテナ起動
docker compose up -d frontend

# ブラウザで確認
# 1. http://localhost:5173/auth/register で会員登録
# 2. カート追加 → 注文作成
# 3. ヘッダーの "Orders" リンクをクリック
# 4. 注文履歴ページが表示されること
# 5. 注文がステータス別に色分けされていること
# 6. 「詳細を見る」ボタンから注文詳細に遷移できること
```

---

## バックエンド実装タスク

### Task 4-BE-1: Order エンティティに user フィールド追加

**ファイル**: `backend/src/main/java/com/example/aiec/entity/Order.java`

**挿入位置**: `sessionId`フィールドの直後

**コード**:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;
```

**import追加**:
```java
import com.example.aiec.entity.User;
```

**参考**: Task1で作成された`User`エンティティ（`backend/src/main/java/com/example/aiec/entity/User.java`）

---

### Task 4-BE-2: OrderRepository に会員注文取得メソッド追加

**ファイル**: `backend/src/main/java/com/example/aiec/repository/OrderRepository.java`

**挿入位置**: 既存メソッド（`findBySessionIdOrderByCreatedAtDesc`）の直後

**コード**:
```java
/**
 * 会員IDで注文一覧を取得（作成日時降順）
 */
List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
```

**参考**: 同じファイル内の`findBySessionIdOrderByCreatedAtDesc`メソッド

---

### Task 4-BE-3: OrderService に UserRepository 依存関係追加

**ファイル**: `backend/src/main/java/com/example/aiec/service/OrderService.java`

**挿入位置**: クラスの`@RequiredArgsConstructor`アノテーションの直後、既存フィールド（`orderRepository`, `cartRepository`, `inventoryService`）の末尾

**コード**:
```java
private final UserRepository userRepository;
```

**import追加**:
```java
import com.example.aiec.repository.UserRepository;
```

**参考**: 同じファイル内の既存フィールド宣言パターン

---

### Task 4-BE-4: OrderService に注文履歴取得メソッド追加

**ファイル**: `backend/src/main/java/com/example/aiec/service/OrderService.java`

**挿入位置**: クラス内の既存メソッド（`getOrderByNumber`など）の末尾

**コード**:
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

**参考**: 同じファイル内の既存メソッド（`getOrderById`等）の@Transactionalとストリーム処理パターン

---

### Task 4-BE-5: OrderService の createOrder メソッド修正（userId対応）

**ファイル**: `backend/src/main/java/com/example/aiec/service/OrderService.java`

**変更内容**: メソッドシグネチャと実装を修正

**変更前**:
```java
@Transactional
public OrderDto createOrder(String sessionId, String cartId) {
```

**変更後**:
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
```

**挿入位置**: `order.setStatus(Order.OrderStatus.PENDING);`の直後

**コード**:
```java
// 会員IDを設定（ログイン時のみ）
if (userId != null) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "ユーザーが見つかりません"));
    order.setUser(user);
}
```

**参考**: Task3の`CartService.mergeGuestCart`メソッドでのUserエンティティ取得パターン

---

### Task 4-BE-6: OrderService の getOrderById メソッド修正（権限チェック）

**ファイル**: `backend/src/main/java/com/example/aiec/service/OrderService.java`

**変更内容**: メソッド全体を置き換え

**変更前**:
```java
@Transactional(readOnly = true)
public OrderDto getOrderById(Long id, String sessionId) {
    Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

    // セッションIDが一致するか確認
    if (!order.getSessionId().equals(sessionId)) {
        throw new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません");
    }

    return OrderDto.fromEntity(order);
}
```

**変更後**:
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

**参考**: Task3の権限チェックパターン（ゲスト/会員の分岐）

---

### Task 4-BE-7: OrderService の cancelOrder メソッド修正（権限チェック）

**ファイル**: `backend/src/main/java/com/example/aiec/service/OrderService.java`

**変更内容**: メソッドシグネチャと権限チェック部分を修正

**変更前**:
```java
@Transactional
public OrderDto cancelOrder(Long orderId, String sessionId) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません"));

    // セッションIDが一致するか確認
    if (!order.getSessionId().equals(sessionId)) {
        throw new ResourceNotFoundException("ORDER_NOT_FOUND", "注文が見つかりません");
    }
```

**変更後**:
```java
/**
 * 注文をキャンセル
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
```

**注意**: メソッドの残りの部分（在庫解放など）は変更なし

**参考**: Task 4-BE-6の権限チェックロジック

---

### Task 4-BE-8: OrderController に AuthService 依存関係追加

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**挿入位置**: クラスの`@RequiredArgsConstructor`アノテーションの直後、既存フィールド（`orderService`）の末尾

**コード**:
```java
private final AuthService authService;
```

**import追加**:
```java
import com.example.aiec.service.AuthService;
import com.example.aiec.entity.User;
```

**参考**: Task2の`AuthController`における依存関係注入パターン

---

### Task 4-BE-9: OrderController に extractToken ヘルパーメソッド追加

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
```

**import追加**:
```java
import com.example.aiec.exception.BusinessException;
```

**参考**: Task2の`AuthController`における同様のヘルパーメソッド

---

### Task 4-BE-10: OrderController に注文履歴エンドポイント追加

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**挿入位置**: 既存エンドポイント（`getOrderById`など）の直後

**コード**:
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

**参考**: Task2の`AuthController`における認証ヘッダー処理パターン

---

### Task 4-BE-11: OrderController の createOrder メソッド修正

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**変更内容**: メソッド全体を置き換え

**変更前**:
```java
@PostMapping
public ApiResponse<OrderDto> createOrder(
        @RequestHeader("X-Session-Id") String sessionId,
        @Valid @RequestBody CreateOrderRequest request) {
    OrderDto order = orderService.createOrder(sessionId, request.getCartId());
    return ApiResponse.success(order);
}
```

**変更後**:
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

**参考**: Task3の`CartController.mergeGuestCart`メソッドにおける認証ヘッダーのオプショナル処理パターン

---

### Task 4-BE-12: OrderController の getOrderById メソッド修正

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**変更内容**: メソッド全体を置き換え

**変更前**:
```java
@GetMapping("/{id}")
public ApiResponse<OrderDto> getOrderById(
        @PathVariable Long id,
        @RequestHeader("X-Session-Id") String sessionId) {
    OrderDto order = orderService.getOrderById(id, sessionId);
    return ApiResponse.success(order);
}
```

**変更後**:
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

**参考**: Task 4-BE-11の認証ヘッダー処理パターン

---

### Task 4-BE-13: OrderController の cancelOrder メソッド修正

**ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

**変更内容**: メソッド全体を置き換え

**変更前**:
```java
@PostMapping("/{id}/cancel")
public ApiResponse<OrderDto> cancelOrder(
        @PathVariable Long id,
        @RequestHeader("X-Session-Id") String sessionId) {
    OrderDto order = orderService.cancelOrder(id, sessionId);
    return ApiResponse.success(order);
}
```

**変更後**:
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

**参考**: Task 4-BE-11の認証ヘッダー処理パターン

---

## フロントエンド実装タスク

### Task 4-FE-1: api.ts に注文履歴取得関数追加

**ファイル**: `frontend/src/lib/api.ts`

**挿入位置**: 既存のorder関連API関数（`createOrder`, `getOrderById`など）の直後

**コード**:
```typescript
/**
 * 会員の注文履歴を取得
 */
export async function getOrderHistory(): Promise<ApiResponse<Order[]>> {
  return fetchApi<Order[]>('/api/order/history')
}
```

**参考**: 同じファイル内の既存のAPI関数パターン（`getOrderById`など）

---

### Task 4-FE-2: OrderHistoryPage コンポーネント作成

**ファイル**: `frontend/src/pages/OrderHistoryPage.tsx`（新規作成）

**コード**:
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

**参考**:
- `frontend/src/pages/CartPage.tsx` — ページ構造とローディング/エラーハンドリング
- `frontend/src/pages/OrderDetailPage.tsx` — 注文アイテムの表示パターン
- `frontend/src/contexts/AuthContext.tsx` — `useAuth`フックの使用パターン

---

### Task 4-FE-3: App.tsx にルート追加

**ファイル**: `frontend/src/App.tsx`

**import追加**（ファイル冒頭）:
```typescript
import OrderHistoryPage from './pages/OrderHistoryPage'
```

**挿入位置**: 既存の`<Route>`要素群の中、`/order/:id`ルートの直後

**コード**:
```typescript
<Route path="/order/history" element={<OrderHistoryPage />} />
```

**参考**: 同じファイル内の既存ルート定義パターン

---

### Task 4-FE-4: Layout.tsx に注文履歴リンク追加

**ファイル**: `frontend/src/components/Layout.tsx`

**挿入位置**: ヘッダーの認証状態表示部分（`{isAuthenticated ? (...) : (...)}`ブロック内）

**変更前**:
```typescript
{isAuthenticated ? (
  <>
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

**参考**: 同じファイル内の既存の`<Link>`要素のスタイリング

---

## テスト手順

### 1. 会員注文の作成と履歴表示

```
1. フロントエンド起動: http://localhost:5173
2. 会員登録: /auth/register で新規会員を作成
3. 商品をカートに追加
4. 注文を作成（/order ページ）
5. ヘッダーの "Orders" リンクをクリック
6. 注文履歴ページ(/order/history)が表示される
7. 作成した注文が一覧に表示される
8. 注文ステータスが適切な色で表示される
   - DELIVERED: 緑
   - SHIPPED: 青
   - CANCELLED: 赤
   - その他: グレー
9. 「詳細を見る」をクリックして注文詳細ページに遷移できる
```

### 2. 複数注文の履歴確認

```
1. 同じ会員で2件目の注文を作成
2. /order/history に戻る
3. 2件の注文が作成日時降順で表示される
4. それぞれの注文番号、合計金額、注文日時が正しく表示される
```

### 3. ゲスト注文との分離

```
1. ログアウト
2. ゲストとして商品をカートに追加し注文を作成
3. ログイン
4. /order/history を表示
5. ゲスト注文は表示されないこと（会員注文のみ表示）
```

### 4. 注文履歴へのアクセス制御

```
1. ログアウト状態で /order/history にアクセス
2. 「ログインしてください」メッセージが表示される
3. ログインリンクから /auth/login に遷移できる
```

### 5. 注文詳細の権限チェック（会員注文）

```
1. 会員Aでログインして注文を作成（注文ID=1）
2. 会員Bでログインして注文を作成（注文ID=2）
3. 会員Aでログインして /order/2 にアクセス
4. 404エラー（ORDER_NOT_FOUND）が返される
5. 会員Aでログインして /order/1 にアクセス
6. 正常に注文詳細が表示される
```

### 6. 注文詳細の権限チェック（ゲスト注文）

```
1. ゲスト（sessionId=session-A）で注文を作成（注文ID=3）
2. 別のブラウザまたはシークレットウィンドウ（sessionId=session-B）で /order/3 にアクセス
3. 404エラー（ORDER_NOT_FOUND）が返される
4. 元のブラウザ（sessionId=session-A）で /order/3 にアクセス
5. 正常に注文詳細が表示される
```

### 7. 後方互換性の確認

```
1. ゲスト注文フローが動作すること
   - カート追加 → 注文作成 → 注文詳細表示 → キャンセル
2. 既存の注文完了ページが動作すること
3. 既存の注文詳細ページが動作すること
```

---

## 完了条件

- [ ] バックエンドのコンパイルが成功する
- [ ] フロントエンドのビルドが成功する
- [ ] 会員が注文を作成すると注文に`userId`が設定される
- [ ] ゲストが注文を作成すると注文の`userId`が`null`になる
- [ ] 会員が`/order/history`で自分の注文履歴を取得できる
- [ ] 注文履歴は作成日時降順でソートされている
- [ ] 注文ステータスが適切な色で表示される
- [ ] 会員は自分の注文のみ閲覧できる（他会員の注文は404）
- [ ] ゲストは自分のsessionIdの注文のみ閲覧できる（他ゲストの注文は404）
- [ ] ログアウト状態で`/order/history`にアクセスするとログイン誘導メッセージが表示される
- [ ] ヘッダーに "Orders" リンクが表示される（ログイン時のみ）
- [ ] 既存のゲスト注文フローが正常に動作する
