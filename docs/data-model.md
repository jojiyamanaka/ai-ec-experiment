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
| **User** | **会員** | **`users`** |
| **AuthToken** | **認証トークン** | **`auth_tokens`** |
| **OperationHistory** | **操作履歴** | **`operation_histories`** |

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

セッション単位のカートを管理します。会員ログイン後は会員IDも紐付けます。

**TypeScript 型定義**:
```typescript
interface Cart {
  id: number              // カートID（主キー）
  sessionId: string       // セッションID（UUID v4）
  userId: number | null   // 会員ID（外部キー、ログイン時のみ）
  createdAt: string       // 作成日時（ISO 8601形式）
  updatedAt: string       // 更新日時（ISO 8601形式）
}
```

**データベーススキーマ**:
```sql
CREATE TABLE carts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id VARCHAR(36) NOT NULL UNIQUE,
  user_id INTEGER,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_carts_session_id ON carts(session_id);
CREATE INDEX idx_carts_user_id ON carts(user_id);
```

**制約**:
- `session_id`: 必須、ユニーク制約、UUID v4形式
- `user_id`: オプション（ゲストの場合は null、ログイン時のみ設定）

**ビジネスルール**:
- **ゲストカート**: `session_id` のみで識別（`user_id = null`）
- **会員カート**: `session_id` + `user_id` で識別（ログイン時に紐付け、Task3で実装）
- **カート引き継ぎ**: ログイン時にゲストカートを会員カートにマージ（Task3で実装）
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
- `quantity`: 必須、1〜9の整数（**1商品あたり最大9個、CHG-003で実装**）
- `(cart_id, product_id)`: 複合ユニーク制約（同一カート内で同じ商品は1行のみ）
- カスケード削除: カート削除時にカートアイテムも削除

**ビジネスルール**:
- 数量を 0 にすると自動的に削除
- カートに追加時、既に同じ商品があれば数量を加算
- **1商品あたり最大9個まで設定可能**（CHG-003で実装）
- 在庫数を超える数量は設定不可
- **非公開商品の自動除外**: 商品が非公開になった場合、カートから自動削除（CHG-002で実装）

**参照**:
- 業務要件: [requirements.md](./requirements.md) - カート機能

---

### Order（注文）

注文のヘッダ情報を管理します。会員注文とゲスト注文を区別します。

**TypeScript 型定義**:
```typescript
type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

interface Order {
  id: number              // 注文ID（主キー）
  orderNumber: string     // 注文番号（ORD-xxxxxxxxxx形式）
  sessionId: string       // セッションID
  userId: number | null   // 会員ID（外部キー、会員注文の場合のみ）
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
  user_id INTEGER,
  total_price INTEGER NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_orders_session_id ON orders(session_id);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_status ON orders(status);
```

**制約**:
- `order_number`: 必須、ユニーク制約、`ORD-xxxxxxxxxx` 形式
- `user_id`: オプション（ゲスト注文の場合は null、会員注文の場合のみ設定）
- `status`: 必須、列挙型（PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED）
- `total_price`: 必須、0以上の整数

**ビジネスルール**:
- **ゲスト注文**: `session_id` のみで識別（`user_id = null`）
- **会員注文**: `session_id` + `user_id` で識別（Task4で実装）
- **権限チェック**:
  - 会員注文: `user_id` が一致する会員のみアクセス可能
  - ゲスト注文: `session_id` が一致するセッションのみアクセス可能
- **退会時の匿名化**: 会員退会時は `user_id = null` に更新（注文履歴は保持）
- 注文番号は自動生成（0埋め10桁連番）
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

### User（会員）

会員情報を管理します。

**TypeScript 型定義**:
```typescript
interface User {
  id: number              // 会員ID（主キー）
  email: string           // メールアドレス（ログインID）
  displayName: string     // 表示名
  role: 'CUSTOMER' | 'ADMIN' // ロール（CUSTOMER: 一般顧客、ADMIN: 管理者）
  createdAt: string       // 作成日時（ISO 8601形式）
}
```

**データベーススキーマ**:
```sql
CREATE TABLE users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  email VARCHAR(255) NOT NULL UNIQUE,
  display_name VARCHAR(100) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
```

**PostgreSQL + Flyway 移行メモ（`is_active` カラム）**:
- 現行の SQLite 運用では `users.is_active` が後付け追加のため、環境によっては `NULL` が残る可能性がある。
- PostgreSQL 移行時に Flyway で `is_active` の補正を必ず実施する（`NULL` 埋め + `DEFAULT true` + `NOT NULL`）。
- 例（Flyway SQL のイメージ）:
  ```sql
  ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN;
  UPDATE users SET is_active = TRUE WHERE is_active IS NULL;
  ALTER TABLE users ALTER COLUMN is_active SET DEFAULT TRUE;
  ALTER TABLE users ALTER COLUMN is_active SET NOT NULL;
  ```

**制約**:
- `email`: 必須、ユニーク制約、最大255文字、メール形式
- `display_name`: 必須、最大100文字
- `password_hash`: 必須、BCryptハッシュ（60文字）
- `role`: 必須、列挙型（CUSTOMER, ADMIN）、デフォルト `CUSTOMER`

**ビジネスルール**:
- パスワードは BCrypt でハッシュ化して保存（平文保存禁止）
- 会員登録時は自動的に `CUSTOMER` ロールが割り当てられる
- 管理者（`ADMIN`）ロールは手動で設定（DB直接更新または専用ツール）
- メールアドレスは一意（同じメールで複数アカウント不可）

**参照**:
- 業務要件: [requirements.md](./requirements.md) - 会員機能

---

### AuthToken（認証トークン）

会員の認証トークンを管理します。トークンベース認証を実現します。

**TypeScript 型定義**:
```typescript
interface AuthToken {
  id: number              // トークンID（主キー）
  userId: number          // 会員ID（外部キー）
  tokenHash: string       // トークンハッシュ（SHA-256、64文字）
  expiresAt: string       // 有効期限（ISO 8601形式）
  isRevoked: boolean      // 失効フラグ
  revokedAt: string | null // 失効日時（ISO 8601形式）
  createdAt: string       // 作成日時（ISO 8601形式）
}
```

**データベーススキーマ**:
```sql
CREATE TABLE auth_tokens (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  is_revoked BOOLEAN NOT NULL DEFAULT false,
  revoked_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_tokens_user_id ON auth_tokens(user_id);
CREATE INDEX idx_auth_tokens_token_hash ON auth_tokens(token_hash);
CREATE INDEX idx_auth_tokens_expires_at ON auth_tokens(expires_at);
```

**制約**:
- `token_hash`: 必須、ユニーク制約、SHA-256ハッシュ（64文字）
- `expires_at`: 必須、作成時刻 + 7日
- `is_revoked`: 必須、デフォルト `false`
- カスケード削除: ユーザー削除時にトークンも削除

**ビジネスルール**:
- **トークン生成**: ログイン・登録時に UUID v4 を生成し、SHA-256 でハッシュ化して保存
- **クライアント送信**: 生のUUID（36文字）を `Authorization: Bearer <token>` ヘッダーで送信
- **DB保存**: SHA-256ハッシュ（64文字）のみ保存（平文保存禁止）
- **有効期限**: 7日間（作成時刻から計算）
- **失効処理**: ログアウト時は `is_revoked = true` に更新（物理削除しない）
- **履歴保持**: 失効済みトークンもログイン履歴として保持
- **有効性チェック**: `is_revoked = false` かつ `expires_at > 現在時刻`

**参照**:
- 業務要件: [requirements.md](./requirements.md) - 会員機能

---

### OperationHistory（操作履歴）

システム内の重要な操作を記録します。監査ログとして活用します。

**TypeScript 型定義**:
```typescript
type EventType = 'LOGIN_SUCCESS' | 'LOGIN_FAILURE' | 'AUTHORIZATION_ERROR' | 'ADMIN_ACTION';

interface OperationHistory {
  id: number              // 履歴ID（主キー）
  eventType: EventType    // イベント種別
  details: string         // イベント詳細
  userId: number | null   // 対象ユーザーID（ログイン失敗時はnull）
  userEmail: string | null // 対象ユーザーのメールアドレス
  ipAddress: string | null // IPアドレス（将来拡張用）
  requestPath: string | null // リクエストパス（例: /api/order/123/ship）
  createdAt: string       // 発生日時（ISO 8601形式）
}
```

**データベーススキーマ**:
```sql
CREATE TABLE operation_histories (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  event_type VARCHAR(50) NOT NULL,
  details VARCHAR(500) NOT NULL,
  user_id INTEGER,
  user_email VARCHAR(255),
  ip_address VARCHAR(45),
  request_path VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_operation_histories_event_type ON operation_histories(event_type);
CREATE INDEX idx_operation_histories_user_id ON operation_histories(user_id);
CREATE INDEX idx_operation_histories_created_at ON operation_histories(created_at);
```

**制約**:
- `event_type`: 必須、列挙型（LOGIN_SUCCESS, LOGIN_FAILURE, AUTHORIZATION_ERROR, ADMIN_ACTION）
- `details`: 必須、最大500文字
- `user_id`: オプション（ログイン失敗時などはnull）
- `user_email`: オプション（ログイン失敗時の記録用）

**ビジネスルール**:
- **記録対象**:
  - `LOGIN_SUCCESS`: ログイン成功
  - `LOGIN_FAILURE`: ログイン失敗（パスワード誤り、アカウント不存在）
  - `AUTHORIZATION_ERROR`: 権限不足（一般会員が管理APIにアクセス）
  - `ADMIN_ACTION`: 管理操作（商品更新、注文ステータス変更）
- **トランザクション**: `REQUIRES_NEW` で親トランザクション失敗時も記録
- **削除禁止**: 監査証跡として永続保存
- **検索**: イベント種別、ユーザーID、日時で検索可能

**参照**:
- 業務要件: [requirements.md](./requirements.md) - 会員機能

---

## エンティティ関連図（ER図）

```
┌─────────────────┐
│      users      │
│─────────────────│
│ id (PK)         │
│ email (UNIQUE)  │
│ display_name    │
│ password_hash   │
│ role            │
└────┬────────────┘
     │
     │ 1:N
     │
┌────▼─────────────┐
│  auth_tokens     │
│──────────────────│
│ id (PK)          │
│ user_id (FK)     │
│ token_hash       │
│ expires_at       │
│ is_revoked       │
└──────────────────┘


┌──────────────────────┐
│ operation_histories  │
│──────────────────────│
│ id (PK)              │
│ event_type           │
│ details              │
│ user_id (nullable)   │
│ user_email           │
│ request_path         │
└──────────────────────┘
(独立したログテーブル)


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
│ session_id   │◀─────┐
│ user_id (FK) │      │ (Task3で追加: ゲスト/会員共存)
└──────────────┘      │
                      │
               ┌──────┴────┐
               │   users   │
               └───────────┘


┌─────────────────┐
│     orders      │
│─────────────────│
│ id (PK)         │
│ order_number    │
│ session_id      │
│ user_id (FK)    │◀────┐ (Task4で追加: 会員注文)
│ total_price     │     │
│ status          │     │
└────────┬────────┘     │
         │              │
         │ 1:N      ┌───┴────┐
         │          │ users  │
┌────────▼────────┐ └────────┘
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

**会員関連**:
- `users` → `auth_tokens`: 1対N（1人の会員は複数のトークンを持つ）
- `users` → `carts`: 1対1（1人の会員は1つのカートを持つ、Task3で実装）
- `users` → `orders`: 1対N（1人の会員は複数の注文を持つ、Task4で実装）
- `operation_histories`: 独立したログテーブル（外部キーなし、user_idは参照のみ）

**商品・カート関連**:
- `products` → `cart_items`: 1対N（1つの商品は複数のカートアイテムに追加される）
- `carts` → `cart_items`: 1対N（1つのカートは複数のカートアイテムを持つ）
- `products` → `stock_reservations`: 1対N（1つの商品は複数の引当を持つ）

**注文関連**:
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
   - 注文番号生成（ORD-xxxxxxxxxx）
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
