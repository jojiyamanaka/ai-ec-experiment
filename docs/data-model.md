# データモデル

**目的**: AIを活用したECサイトのデータモデル（エンティティ、テーブル構造、型定義）を定義する
**スコープ**: エンティティ定義、データベーススキーマ、TypeScript型定義、エンティティ関連図

**関連ドキュメント**:
- [技術仕様](./SPEC.md) - 技術方針・アーキテクチャ
- [業務要件](./requirements.md) - ビジネスルール
- [API仕様](./ui/api-spec.md) - APIリクエスト/レスポンス型

---

## エンティティ一覧

| エンティティ | 説明 | テーブル名 |
|------------|------|-----------|
| Product | 商品 | `products` |
| Cart | カート | `carts` |
| CartItem | カートアイテム | `cart_items` |
| Order | 注文 | `orders` |
| OrderItem | 注文アイテム | `order_items` |
| StockReservation | 在庫引当 | `stock_reservations` |

---

## エンティティ詳細

### Product（商品）

商品マスタ。EC サイトで販売する商品の情報を管理します。

**TypeScript 型定義**:
```typescript
interface Product {
  id: number              // 商品ID（主キー）
  name: string            // 商品名
  price: number           // 価格（円、税込）
  image: string           // 商品画像URL
  description: string     // 商品説明
  stock: number           // 在庫数（物理在庫）
  isPublished: boolean    // 公開状態（true: 公開、false: 非公開）
}
```

**データベーススキーマ**:
```sql
CREATE TABLE products (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name VARCHAR(255) NOT NULL,
  price INTEGER NOT NULL,
  image VARCHAR(500),
  description TEXT,
  stock INTEGER NOT NULL DEFAULT 0,
  is_published BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**制約**:
- `name`: 必須、最大255文字
- `price`: 必須、0以上の整数
- `stock`: 必須、0以上の整数
- `is_published`: 必須、デフォルト `true`

**ビジネスルール**:
- `is_published = false` の商品は商品一覧・検索結果に表示しない
- `stock` が 0 の商品は「売り切れ」として表示
- 在庫引当により、実際に購入可能な在庫は `stock - Σ(引当数量)` となる

**参照**:
- 業務要件: [requirements.md](./requirements.md) - 在庫状態のルール

---

### Cart（カート）

セッション単位のカートを管理します。

**TypeScript 型定義**:
```typescript
interface Cart {
  id: number              // カートID（主キー）
  sessionId: string       // セッションID（UUID v4）
  createdAt: string       // 作成日時（ISO 8601形式）
  updatedAt: string       // 更新日時（ISO 8601形式）
}
```

**データベーススキーマ**:
```sql
CREATE TABLE carts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id VARCHAR(36) NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_carts_session_id ON carts(session_id);
```

**制約**:
- `session_id`: 必須、ユニーク制約
- セッションIDは UUID v4 形式（例: `550e8400-e29b-41d4-a716-446655440000`）

**ビジネスルール**:
- カートは `session_id` でユーザーを識別
- カートアイテムは `cart_items` テーブルで管理
- 注文確定時にカートアイテムをクリア

**参照**:
- 業務要件: [requirements.md](./requirements.md) - カート機能

---

### CartItem（カートアイテム）

カートに入っている商品と数量を管理します。

**TypeScript 型定義**:
```typescript
interface CartItem {
  id: number              // カートアイテムID（主キー）
  cartId: number          // カートID（外部キー）
  productId: number       // 商品ID（外部キー）
  quantity: number        // 数量
  product: Product        // 商品情報（結合して取得）
}
```

**データベーススキーマ**:
```sql
CREATE TABLE cart_items (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  cart_id INTEGER NOT NULL,
  product_id INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id),
  UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items(product_id);
```

**制約**:
- `quantity`: 必須、1以上の整数
- `(cart_id, product_id)`: 複合ユニーク制約（同一カート内で同じ商品は1行のみ）
- カスケード削除: カート削除時にカートアイテムも削除

**ビジネスルール**:
- 数量を 0 にすると自動的に削除
- カートに追加時、既に同じ商品があれば数量を加算
- 在庫数を超える数量は設定不可

**参照**:
- 業務要件: [requirements.md](./requirements.md) - カート機能

---

### Order（注文）

注文のヘッダ情報を管理します。

**TypeScript 型定義**:
```typescript
type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

interface Order {
  id: number              // 注文ID（主キー）
  orderNumber: string     // 注文番号（ORD-YYYYMMDD-XXX形式）
  sessionId: string       // セッションID
  totalPrice: number      // 合計金額（円）
  status: OrderStatus     // 注文状態
  createdAt: string       // 作成日時（ISO 8601形式）
  updatedAt: string       // 更新日時（ISO 8601形式）
}
```

**データベーススキーマ**:
```sql
CREATE TABLE orders (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  order_number VARCHAR(20) NOT NULL UNIQUE,
  session_id VARCHAR(36) NOT NULL,
  total_price INTEGER NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_session_id ON orders(session_id);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_status ON orders(status);
```

**制約**:
- `order_number`: 必須、ユニーク制約、`ORD-YYYYMMDD-XXX` 形式
- `status`: 必須、列挙型（PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED）
- `total_price`: 必須、0以上の整数

**ビジネスルール**:
- 注文番号は自動生成（日付 + 連番）
- 注文確定時は PENDING 状態で作成
- 状態遷移は定義されたルールに従う（後述）

**参照**:
- 業務要件: [requirements.md](./requirements.md) - 注文の状態遷移
- 詳細仕様: [specs/order.md](./specs/order.md)

---

### OrderItem（注文アイテム）

注文に含まれる商品と数量を管理します。

**TypeScript 型定義**:
```typescript
interface OrderItem {
  id: number              // 注文アイテムID（主キー）
  orderId: number         // 注文ID（外部キー）
  productId: number       // 商品ID（外部キー）
  productName: string     // 商品名（注文時点のスナップショット）
  price: number           // 単価（注文時点のスナップショット）
  quantity: number        // 数量
  subtotal: number        // 小計（price × quantity）
  product: Product        // 商品情報（結合して取得）
}
```

**データベーススキーマ**:
```sql
CREATE TABLE order_items (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  order_id INTEGER NOT NULL,
  product_id INTEGER NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  price INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  subtotal INTEGER NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
```

**制約**:
- `quantity`: 必須、1以上の整数
- `price`: 必須、0以上の整数
- `subtotal`: 必須、`price × quantity` と一致
- カスケード削除: 注文削除時に注文アイテムも削除

**ビジネスルール**:
- 注文時点の商品名・価格をスナップショットとして保存
- 後から商品マスタが変更されても注文データは変わらない
- `subtotal` は自動計算（`price × quantity`）

**参照**:
- 業務要件: [requirements.md](./requirements.md) - 注文機能
- 詳細仕様: [specs/order.md](./specs/order.md)

---

### StockReservation（在庫引当）

在庫の引き当て（仮引当・本引当）を管理します。

**TypeScript 型定義**:
```typescript
type ReservationType = 'TENTATIVE' | 'COMMITTED';

interface StockReservation {
  id: number              // 引当ID（主キー）
  productId: number       // 商品ID（外部キー）
  quantity: number        // 引当数量
  type: ReservationType   // 引当種別（TENTATIVE: 仮引当、COMMITTED: 本引当）
  sessionId: string       // セッションID（仮引当の場合）
  orderId: number | null  // 注文ID（本引当の場合）
  expiresAt: string | null // 有効期限（仮引当の場合、ISO 8601形式）
  createdAt: string       // 作成日時（ISO 8601形式）
}
```

**データベーススキーマ**:
```sql
CREATE TABLE stock_reservations (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  product_id INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  type VARCHAR(20) NOT NULL,
  session_id VARCHAR(36),
  order_id INTEGER,
  expires_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES products(id),
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_stock_reservations_product_id ON stock_reservations(product_id);
CREATE INDEX idx_stock_reservations_session_id ON stock_reservations(session_id);
CREATE INDEX idx_stock_reservations_order_id ON stock_reservations(order_id);
CREATE INDEX idx_stock_reservations_expires_at ON stock_reservations(expires_at);
```

**制約**:
- `type`: 必須、列挙型（TENTATIVE, COMMITTED）
- `quantity`: 必須、1以上の整数
- 仮引当（TENTATIVE）の場合:
  - `session_id`: 必須
  - `expires_at`: 必須（作成時刻 + 30分）
  - `order_id`: NULL
- 本引当（COMMITTED）の場合:
  - `order_id`: 必須
  - `session_id`: NULL
  - `expires_at`: NULL

**ビジネスルール**:
- **仮引当（TENTATIVE）**: カート追加時に作成。30分で自動失効。
- **本引当（COMMITTED）**: 注文確定時に仮引当から変換。`products.stock` を減少。
- **本引当解除**: 注文キャンセル時に削除。`products.stock` を戻す。
- **有効在庫**: `products.stock - Σ(有効な引当数量)`
- **定期クリーンアップ**: 5分ごとに期限切れの仮引当を削除

**参照**:
- 業務要件: [requirements.md](./requirements.md) - 在庫状態のルール
- 詳細仕様: [specs/inventory.md](./specs/inventory.md) - 在庫引当の詳細

---

## エンティティ関連図（ER図）

```
┌─────────────────┐
│    products     │
│─────────────────│
│ id (PK)         │
│ name            │
│ price           │
│ image           │
│ description     │
│ stock           │
│ is_published    │
└────────┬────────┘
         │
         │ 1:N
         │
    ┌────┴────────────────────────────────┐
    │                                     │
┌───▼────────────┐              ┌────────▼─────────────┐
│  cart_items    │              │ stock_reservations   │
│────────────────│              │──────────────────────│
│ id (PK)        │              │ id (PK)              │
│ cart_id (FK)   │              │ product_id (FK)      │
│ product_id (FK)│              │ quantity             │
│ quantity       │              │ type (TENTATIVE/     │
└───┬────────────┘              │      COMMITTED)      │
    │                           │ session_id           │
    │ N:1                       │ order_id (FK)        │
    │                           │ expires_at           │
┌───▼──────────┐                └──────────────────────┘
│    carts     │
│──────────────│
│ id (PK)      │
│ session_id   │
└──────────────┘


┌─────────────────┐
│     orders      │
│─────────────────│
│ id (PK)         │
│ order_number    │
│ session_id      │
│ total_price     │
│ status          │
└────────┬────────┘
         │
         │ 1:N
         │
┌────────▼────────┐
│  order_items    │
│─────────────────│
│ id (PK)         │
│ order_id (FK)   │
│ product_id (FK) │◀────┐
│ product_name    │     │ N:1
│ price           │     │
│ quantity        │     │
│ subtotal        │     │
└─────────────────┘     │
                        │
                  ┌─────┴────────┐
                  │   products   │
                  │──────────────│
                  │ id (PK)      │
                  └──────────────┘
```

**関連の説明**:
- `products` → `cart_items`: 1対N（1つの商品は複数のカートアイテムに追加される）
- `carts` → `cart_items`: 1対N（1つのカートは複数のカートアイテムを持つ）
- `products` → `stock_reservations`: 1対N（1つの商品は複数の引当を持つ）
- `orders` → `order_items`: 1対N（1つの注文は複数の注文アイテムを持つ）
- `products` → `order_items`: 1対N（1つの商品は複数の注文アイテムに含まれる）
- `orders` → `stock_reservations`: 1対N（1つの注文は複数の本引当を持つ）

---

## データ整合性とバリデーション

### 在庫の整合性

**有効在庫の計算**:
```sql
SELECT
  p.id,
  p.name,
  p.stock AS physical_stock,
  COALESCE(SUM(sr.quantity), 0) AS reserved_stock,
  p.stock - COALESCE(SUM(sr.quantity), 0) AS available_stock
FROM products p
LEFT JOIN stock_reservations sr
  ON p.id = sr.product_id
  AND (
    (sr.type = 'TENTATIVE' AND sr.expires_at > CURRENT_TIMESTAMP)
    OR sr.type = 'COMMITTED'
  )
GROUP BY p.id;
```

**チェック**:
- カート追加時: `available_stock >= 追加数量`
- 注文確定時: `available_stock >= 注文数量`
- 在庫数変更時: `stock >= Σ(本引当数量)`

### 価格の整合性

**注文アイテムの小計チェック**:
```sql
-- 小計が正しく計算されているか検証
SELECT * FROM order_items
WHERE subtotal != price * quantity;
```

**注文合計金額のチェック**:
```sql
-- 注文合計金額が注文アイテムの合計と一致するか検証
SELECT
  o.id,
  o.total_price,
  SUM(oi.subtotal) AS calculated_total
FROM orders o
JOIN order_items oi ON o.id = oi.order_id
GROUP BY o.id
HAVING o.total_price != SUM(oi.subtotal);
```

### セッションIDの形式チェック

**UUID v4 形式の検証**:
```typescript
const UUID_V4_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function isValidSessionId(sessionId: string): boolean {
  return UUID_V4_REGEX.test(sessionId);
}
```

---

## データのライフサイクル

### カートデータ

```
1. セッションID生成（フロントエンド）
   ↓
2. カート作成（初回カート追加時）
   ↓
3. カートアイテム追加/更新/削除
   ↓
4. 注文確定
   ↓
5. カートアイテム削除（クリア）
```

### 在庫引当データ

```
1. 仮引当作成（カート追加時）
   - type = TENTATIVE
   - expires_at = now + 30分
   ↓
2. 仮引当更新（カート数量変更時）
   - quantity を更新
   ↓
3. 仮引当削除（カート削除時）
   ↓
4. 本引当変換（注文確定時）
   - type = COMMITTED
   - order_id を設定
   - products.stock を減少
   ↓
5. 本引当解除（注文キャンセル時）
   - 削除
   - products.stock を戻す
```

### 注文データ

```
1. 注文作成（PENDING）
   - カート内容から注文アイテム作成
   - 注文番号生成（ORD-YYYYMMDD-XXX）
   ↓
2. 注文確認（CONFIRMED）
   - 管理者が確認
   ↓
3. 発送（SHIPPED）
   - 商品発送
   ↓
4. 配達完了（DELIVERED）
   - 配達完了

   ※ PENDING/CONFIRMED → CANCELLED も可能
```

---

## 参照クエリ例

### カート内容の取得

```sql
SELECT
  ci.id,
  ci.quantity,
  p.id AS product_id,
  p.name,
  p.price,
  p.image,
  p.description,
  p.stock,
  p.is_published,
  (p.price * ci.quantity) AS subtotal
FROM cart_items ci
JOIN carts c ON ci.cart_id = c.id
JOIN products p ON ci.product_id = p.id
WHERE c.session_id = ?
  AND p.is_published = true;
```

### 注文詳細の取得

```sql
SELECT
  o.id,
  o.order_number,
  o.total_price,
  o.status,
  o.created_at,
  oi.id AS item_id,
  oi.product_name,
  oi.price,
  oi.quantity,
  oi.subtotal
FROM orders o
JOIN order_items oi ON o.id = oi.order_id
WHERE o.order_number = ?;
```

### 商品の有効在庫確認

```sql
SELECT
  p.id,
  p.name,
  p.stock,
  COALESCE(SUM(
    CASE
      WHEN sr.type = 'COMMITTED' THEN sr.quantity
      WHEN sr.type = 'TENTATIVE' AND sr.expires_at > CURRENT_TIMESTAMP THEN sr.quantity
      ELSE 0
    END
  ), 0) AS reserved,
  p.stock - COALESCE(SUM(
    CASE
      WHEN sr.type = 'COMMITTED' THEN sr.quantity
      WHEN sr.type = 'TENTATIVE' AND sr.expires_at > CURRENT_TIMESTAMP THEN sr.quantity
      ELSE 0
    END
  ), 0) AS available
FROM products p
LEFT JOIN stock_reservations sr ON p.id = sr.product_id
WHERE p.id = ?
GROUP BY p.id;
```

---

## 関連資料

- **技術仕様**: [SPEC.md](../SPEC.md) - データベース設定
- **業務要件**: [requirements.md](./requirements.md) - ビジネスルール
- **API仕様**: [ui/api-spec.md](./ui/api-spec.md) - リクエスト/レスポンス型
- **詳細仕様**:
  - [在庫管理](./specs/inventory.md) - 在庫引当の詳細
  - [注文管理](./specs/order.md) - 注文フローの詳細
  - [商品ドメイン](./specs/product.md) - 価格計算の詳細
