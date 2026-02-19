# データモデル

**目的**: AIを活用したECサイトのデータモデル（エンティティ、テーブル構造、型定義）を定義する

**関連ドキュメント**:
- [技術仕様](./SPEC.md) - 技術方針・アーキテクチャ
- [業務要件](./requirements.md) - ビジネスルール
- [API仕様](./ui/api-spec.md) - APIリクエスト/レスポンス型

---

## データベース

- **DBMS**: PostgreSQL 16
- **ORM**: Hibernate（Spring Boot 3.4.2）
- **マイグレーション**: Flyway
- **スキーマ定義**: `backend/src/main/resources/db/flyway/V1__create_schema.sql`

---

## 共通設計

### 監査カラム

主要テーブルに定義される共通カラム:

| カラム名 | 型 | 説明 |
|---------|-----|------|
| `created_at` | `TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP` | 作成日時 |
| `created_by_type` | `VARCHAR(50)` | 作成者種別（USER / BO_USER / SYSTEM） |
| `created_by_id` | `BIGINT` | 作成者ID |
| `updated_at` | `TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP` | 更新日時（トリガーで自動更新） |
| `updated_by_type` | `VARCHAR(50)` | 更新者種別 |
| `updated_by_id` | `BIGINT` | 更新者ID |
| `is_deleted` | `BOOLEAN NOT NULL DEFAULT FALSE` | 論理削除フラグ |
| `deleted_at` | `TIMESTAMP WITH TIME ZONE` | 削除日時 |
| `deleted_by_type` | `VARCHAR(50)` | 削除者種別 |
| `deleted_by_id` | `BIGINT` | 削除者ID |

**適用テーブル**: `users`, `auth_tokens`, `products`, `stock_reservations`, `orders`, `order_items`, `operation_histories`, `bo_users`, `bo_auth_tokens`, `inventory_adjustments`

**非適用テーブル**: `carts`, `cart_items`（`created_at` / `updated_at` のみ）

### updated_at 自動更新トリガー

全テーブルに `BEFORE UPDATE` トリガーとして設定（`update_updated_at_column()` 関数）。

---

## エンティティ一覧

| エンティティ | テーブル | 説明 |
|------------|---------|------|
| Product | `products` | 商品マスタ |
| Cart | `carts` | セッション単位のカート |
| CartItem | `cart_items` | カート内商品 |
| Order | `orders` | 注文ヘッダ |
| OrderItem | `order_items` | 注文内商品 |
| StockReservation | `stock_reservations` | 在庫引当（仮引当/本引当） |
| User | `users` | 会員 |
| AuthToken | `auth_tokens` | 会員認証トークン |
| OperationHistory | `operation_histories` | 操作履歴（監査ログ） |
| BoUser | `bo_users` | 管理者ユーザー |
| BoAuthToken | `bo_auth_tokens` | 管理者認証トークン |
| InventoryAdjustment | `inventory_adjustments` | 在庫調整履歴 |
| OutboxEvent | `outbox_events` | 非同期イベント（Transactional Outbox） |

---

## エンティティ詳細

### Product（商品）

```sql
CREATE TABLE products (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(2000),
  price NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
  stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
  image VARCHAR(500),
  is_published BOOLEAN NOT NULL DEFAULT TRUE,
  -- 監査カラム（共通設計参照）
);
```

**制約**: name必須(255字), price必須(0以上), stock必須(0以上), is_published必須(デフォルトTRUE)
**ルール**: `is_published = false` → 一覧・検索非表示。`stock = 0` → 「売り切れ」表示。有効在庫 = `stock - Σ(引当数量)`

### Cart（カート）

```sql
CREATE TABLE carts (
  id BIGSERIAL PRIMARY KEY,
  session_id VARCHAR(255) NOT NULL UNIQUE,
  user_id BIGINT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
```

**ルール**: ゲスト → `session_id` のみ(`user_id = null`)。会員 → `session_id + user_id`。ログイン時にカートマージ。注文確定時にクリア。

### CartItem（カートアイテム）

```sql
CREATE TABLE cart_items (
  id BIGSERIAL PRIMARY KEY,
  cart_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id)
);
```

**制約**: quantity必須(1以上)、(cart_id, product_id)複合ユニーク、最大9（アプリ層制御）、カスケード削除

### Order（注文）

```sql
CREATE TABLE orders (
  id BIGSERIAL PRIMARY KEY,
  order_number VARCHAR(50) NOT NULL UNIQUE,
  user_id BIGINT,
  session_id VARCHAR(255),
  total_price NUMERIC(10, 2) NOT NULL CHECK (total_price >= 0),
  status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
  -- 監査カラム（共通設計参照）
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
```

**制約**: order_number必須(ユニーク, `ORD-xxxxxxxxxx`形式)、status必須(CHECK)

### OrderItem（注文アイテム）

```sql
CREATE TABLE order_items (
  id BIGSERIAL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  product_price NUMERIC(10, 2) NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  subtotal NUMERIC(10, 2) NOT NULL CHECK (subtotal >= 0),
  -- 監査カラム（共通設計参照）
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);
```

**ルール**: 注文時点の商品名・価格をスナップショット保存。商品マスタ変更の影響を受けない。商品削除制限（RESTRICT）。

### StockReservation（在庫引当）

```sql
CREATE TABLE stock_reservations (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  session_id VARCHAR(255),
  user_id BIGINT,
  reservation_type VARCHAR(50) NOT NULL CHECK (reservation_type IN ('TENTATIVE', 'COMMITTED')),
  order_id BIGINT,
  expires_at TIMESTAMP WITH TIME ZONE,
  -- 監査カラム（共通設計参照）
  CONSTRAINT fk_stock_reservations_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_stock_reservations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
```

**ルール**: 仮引当(TENTATIVE) → `expires_at` 必須(+30分)。本引当(COMMITTED) → `order_id` 必須, `expires_at` NULL。詳細は [在庫管理仕様](./specs/inventory.md) 参照。

### User（会員）

```sql
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  -- 監査カラム（共通設計参照）
);
```

**制約**: email必須(ユニーク), password_hash必須(BCrypt 60文字), display_name必須(100字), is_active必須(デフォルトTRUE)
**ルール**: `role` カラムは存在しない（BoUser と分離）。メールアドレスは一意。

### AuthToken（認証トークン）

```sql
CREATE TABLE auth_tokens (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
  -- 監査カラム（共通設計参照）
  CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**ルール**: UUID v4 → SHA-256ハッシュで保存。有効期限7日間。ログアウト時 `is_revoked = true`（ソフトデリート）。

### OperationHistory（操作履歴）

```sql
CREATE TABLE operation_histories (
  id BIGSERIAL PRIMARY KEY,
  operation_type VARCHAR(100) NOT NULL,
  performed_by VARCHAR(255),
  request_path VARCHAR(500),
  details VARCHAR(2000),
  -- 監査カラム（共通設計参照）
);
```

**ルール**: 削除禁止（監査証跡）。`REQUIRES_NEW` トランザクション。

### BoUser（管理者ユーザー）

```sql
CREATE TABLE bo_users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  permission_level VARCHAR(50) NOT NULL DEFAULT 'OPERATOR'
    CHECK (permission_level IN ('SUPER_ADMIN', 'ADMIN', 'OPERATOR')),
  last_login_at TIMESTAMP WITH TIME ZONE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  -- 監査カラム（共通設計参照）
);
```

### BoAuthToken（管理者認証トークン）

AuthToken と同じ仕組み（UUID v4 + SHA-256ハッシュ、有効期限7日間）。顧客トークンと完全に分離。

```sql
CREATE TABLE bo_auth_tokens (
  id BIGSERIAL PRIMARY KEY,
  bo_user_id BIGINT NOT NULL,
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
  -- 監査カラム（共通設計参照）
  CONSTRAINT fk_bo_auth_tokens_bo_user FOREIGN KEY (bo_user_id) REFERENCES bo_users(id) ON DELETE CASCADE
);
```

### InventoryAdjustment（在庫調整履歴）

```sql
CREATE TABLE inventory_adjustments (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT NOT NULL,
  quantity_before INTEGER NOT NULL,
  quantity_after INTEGER NOT NULL,
  quantity_delta INTEGER NOT NULL,
  reason VARCHAR(500) NOT NULL,
  adjusted_by VARCHAR(255) NOT NULL,
  adjusted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 監査カラム（共通設計参照）
  CONSTRAINT fk_inventory_adjustments_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
```

### OutboxEvent（非同期イベント）

```sql
CREATE TYPE outbox_event_status AS ENUM ('PENDING', 'PROCESSING', 'PROCESSED', 'DEAD');

CREATE TABLE outbox_events (
  id BIGSERIAL PRIMARY KEY,
  event_type VARCHAR(100) NOT NULL,
  aggregate_id VARCHAR(255),
  payload JSONB NOT NULL,
  status outbox_event_status NOT NULL DEFAULT 'PENDING',
  retry_count INT NOT NULL DEFAULT 0,
  max_retries INT NOT NULL DEFAULT 3,
  error_message TEXT,
  scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_events_status_scheduled
    ON outbox_events (status, scheduled_at)
    WHERE status = 'PENDING';
```

**ルール**: Transactional Outbox パターン。メイン処理と同一トランザクション内でイベントをINSERT。ポーリングワーカーが PENDING を取得し、各ハンドラにディスパッチ。成功時 PROCESSED、失敗時は再試行（30秒指数バックオフ）→ max_retries到達で DEAD（DLQ相当）。

---

## エンティティ関連

### 会員関連
- `users` → `auth_tokens`: 1:N（CASCADE削除）
- `users` → `carts`: 1:1（SET NULL削除）
- `users` → `orders`: 1:N（SET NULL削除）

### 管理者関連
- `bo_users` → `bo_auth_tokens`: 1:N（CASCADE削除）

### 商品・カート関連
- `products` → `cart_items`: 1:N（CASCADE削除）
- `carts` → `cart_items`: 1:N（CASCADE削除）
- `products` → `stock_reservations`: 1:N（CASCADE削除）
- `products` → `inventory_adjustments`: 1:N（CASCADE削除）

### 注文関連
- `orders` → `order_items`: 1:N（CASCADE削除）
- `products` → `order_items`: 1:N（RESTRICT削除）

---

## データ整合性

### 有効在庫の計算

```sql
SELECT p.id, p.name, p.stock AS physical_stock,
  COALESCE(SUM(sr.quantity), 0) AS reserved_stock,
  p.stock - COALESCE(SUM(sr.quantity), 0) AS available_stock
FROM products p
LEFT JOIN stock_reservations sr ON p.id = sr.product_id AND sr.is_deleted = FALSE
  AND ((sr.reservation_type = 'TENTATIVE' AND sr.expires_at > CURRENT_TIMESTAMP)
       OR sr.reservation_type = 'COMMITTED')
WHERE p.is_deleted = FALSE
GROUP BY p.id;
```

### 注文アイテムの小計チェック

```sql
SELECT * FROM order_items WHERE subtotal != product_price * quantity AND is_deleted = FALSE;
```
