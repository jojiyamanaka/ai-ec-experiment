# CHG-009: SQLite から PostgreSQL への移行 - 技術設計

## 概要

本設計では、SQLite から PostgreSQL への移行手順、スキーマ最適化、データ移行、アプリケーション設定変更を定義します。

---

## 主要な変更点サマリ

### データベース層

| 項目 | SQLite | PostgreSQL | 理由 |
|------|--------|-----------|------|
| **日時型** | `DATETIME` | `TIMESTAMP WITH TIME ZONE` | タイムゾーン対応、UTC保存 |
| **自動採番** | `AUTOINCREMENT` | `BIGSERIAL` | 64bit ID、将来のスケール対応 |
| **真偽値** | `BOOLEAN` (TEXT扱い) | `BOOLEAN NOT NULL` | ネイティブ型、NULL禁止 |
| **金額** | `INTEGER` | `NUMERIC(10, 2)` | 小数点対応（将来の税率変更） |
| **文字列** | `TEXT` (無制限) | `VARCHAR(2000)` | 適切な上限設定 |

### アプリケーション層

| 項目 | 変更前 | 変更後 | 理由 |
|------|--------|--------|------|
| **日時型** | `LocalDateTime` | `Instant` | UTC統一、タイムゾーン明示 |
| **金額型** | `Integer` | `BigDecimal` | 小数点対応 |
| **トランザクション** | `@Transactional` | `@Transactional(rollbackFor = Exception.class)` | チェック例外もロールバック |
| **削除処理** | 物理削除 | 論理削除（`is_deleted = TRUE`） | 監査証跡の保持 |

### 監査カラム（全テーブル追加）

- `created_at`, `created_by_type`, `created_by_id`: 登録日時・アクター種別・ID
- `updated_at`, `updated_by_type`, `updated_by_id`: 更新日時・アクター種別・ID
- `is_deleted`, `deleted_at`, `deleted_by_type`, `deleted_by_id`: 論理削除フラグ・日時・アクター種別・ID

**ActorType**: `USER` (顧客), `BO_USER` (管理者), `SYSTEM` (システム)

---

## 1. PostgreSQL スキーマ設計

### 1.1 バージョンと拡張機能

- **PostgreSQL バージョン**: 16.x
- **使用する拡張機能**:
  - `uuid-ossp`: UUID 生成（将来の ID 戦略として予約）
  - `pg_stat_statements`: スロークエリ分析用（運用時）

### 1.2 データベース構成

```sql
-- データベース作成
CREATE DATABASE ec_app
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'ja_JP.UTF-8'
    LC_CTYPE = 'ja_JP.UTF-8'
    TEMPLATE = template0;

-- 接続ユーザー作成
CREATE USER ec_app_user WITH PASSWORD 'changeme';
GRANT ALL PRIVILEGES ON DATABASE ec_app TO ec_app_user;
```

### 1.3 監査カラムの共通定義

**全テーブルに以下のカラムを追加**:

```sql
-- 登録情報
created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
created_by_type VARCHAR(50),    -- 'USER' | 'BO_USER' | 'SYSTEM'
created_by_id BIGINT,            -- users.id または bo_users.id

-- 更新情報
updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_by_type VARCHAR(50),
updated_by_id BIGINT,

-- 論理削除
is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
deleted_at TIMESTAMP WITH TIME ZONE,
deleted_by_type VARCHAR(50),
deleted_by_id BIGINT
```

#### ActorType の定義

| 値 | 説明 | ID の参照先 |
|----|------|-------------|
| `USER` | 顧客ユーザー | `users.id` |
| `BO_USER` | 管理者ユーザー | `bo_users.id` |
| `SYSTEM` | システム（自動処理） | NULL |

#### インデックス

全テーブルに以下のインデックスを作成:
- `idx_<table>_is_deleted` - 論理削除フラグ
- `idx_<table>_created_by` - 登録者検索用（複合: created_by_type, created_by_id）
- `idx_<table>_updated_by` - 更新者検索用（複合: updated_by_type, updated_by_id）

#### トリガー

`updated_at` の自動更新:

```sql
-- 共通トリガー関数（全テーブルで使用）
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

---

### 1.4 テーブル定義（PostgreSQL 版）

#### users テーブル

**主要な変更点**:
- `DATETIME` → `TIMESTAMP WITH TIME ZONE`（タイムゾーン対応）
- `AUTOINCREMENT` → `BIGSERIAL`（64bit ID）
- `BOOLEAN NOT NULL`（NULL 禁止）
- 監査カラム追加（created_by_type/id, updated_by_type/id, deleted_by_type/id）

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- 監査カラム
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_type VARCHAR(50),
    created_by_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_type VARCHAR(50),
    updated_by_id BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by_type VARCHAR(50),
    deleted_by_id BIGINT
);

CREATE INDEX idx_users_email ON users(email) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_is_active ON users(is_active) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_is_deleted ON users(is_deleted);
CREATE INDEX idx_users_created_by ON users(created_by_type, created_by_id);
CREATE INDEX idx_users_updated_by ON users(updated_by_type, updated_by_id);

-- 更新日時の自動更新トリガー
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

#### auth_tokens テーブル

```sql
CREATE TABLE auth_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,

    -- 監査カラム
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_type VARCHAR(50),
    created_by_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_type VARCHAR(50),
    updated_by_id BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by_type VARCHAR(50),
    deleted_by_id BIGINT,

    CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_tokens_token_hash ON auth_tokens(token_hash) WHERE is_deleted = FALSE;
CREATE INDEX idx_auth_tokens_user_id ON auth_tokens(user_id);
CREATE INDEX idx_auth_tokens_expires_at ON auth_tokens(expires_at);
CREATE INDEX idx_auth_tokens_is_deleted ON auth_tokens(is_deleted);
CREATE INDEX idx_auth_tokens_created_by ON auth_tokens(created_by_type, created_by_id);
CREATE INDEX idx_auth_tokens_updated_by ON auth_tokens(updated_by_type, updated_by_id);

CREATE TRIGGER update_auth_tokens_updated_at
    BEFORE UPDATE ON auth_tokens
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

#### products テーブル

**主要な変更点**:
- `price INTEGER` → `price NUMERIC(10, 2)`（小数点対応、将来の税率変更に備える）
- `description TEXT` → `description VARCHAR(2000)`（適切な上限設定）

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    price NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    image VARCHAR(500),
    is_published BOOLEAN NOT NULL DEFAULT TRUE,

    -- 監査カラム
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_type VARCHAR(50),
    created_by_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_type VARCHAR(50),
    updated_by_id BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by BIGINT
);

CREATE INDEX idx_products_is_published ON products(is_published) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_is_deleted ON products(is_deleted);
CREATE INDEX idx_products_created_by ON products(created_by_type, created_by_id);
CREATE INDEX idx_products_updated_by ON products(updated_by_type, updated_by_id);

CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

#### reservations テーブル（在庫引当）

```sql
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    session_id VARCHAR(255),
    user_id BIGINT,
    reservation_type VARCHAR(50) NOT NULL CHECK (reservation_type IN ('TENTATIVE', 'COMMITTED')),
    order_id BIGINT,
    expires_at TIMESTAMP WITH TIME ZONE,

    -- 監査カラム
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_type VARCHAR(50),
    created_by_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_type VARCHAR(50),
    updated_by_id BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by BIGINT,

    CONSTRAINT fk_reservations_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_reservations_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_reservations_product_id ON reservations(product_id);
CREATE INDEX idx_reservations_session_id ON reservations(session_id);
CREATE INDEX idx_reservations_user_id ON reservations(user_id);
CREATE INDEX idx_reservations_order_id ON reservations(order_id);
CREATE INDEX idx_reservations_type ON reservations(reservation_type);
CREATE INDEX idx_reservations_expires_at ON reservations(expires_at);
CREATE INDEX idx_reservations_is_deleted ON reservations(is_deleted);
CREATE INDEX idx_reservations_created_by ON reservations(created_by_type, created_by_id);
CREATE INDEX idx_reservations_updated_by ON reservations(updated_by_type, updated_by_id);

CREATE TRIGGER update_reservations_updated_at
    BEFORE UPDATE ON reservations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

#### orders テーブル

**主要な変更点**:
- `total_price INTEGER` → `total_price NUMERIC(10, 2)`

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT,
    session_id VARCHAR(255),
    total_price NUMERIC(10, 2) NOT NULL CHECK (total_price >= 0),
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),

    -- 監査カラム
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_type VARCHAR(50),
    created_by_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_type VARCHAR(50),
    updated_by_id BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by BIGINT,

    CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_orders_order_number ON orders(order_number) WHERE is_deleted = FALSE;
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_session_id ON orders(session_id);
CREATE INDEX idx_orders_status ON orders(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_is_deleted ON orders(is_deleted);
CREATE INDEX idx_orders_created_by ON orders(created_by_type, created_by_id);
CREATE INDEX idx_orders_updated_by ON orders(updated_by_type, updated_by_id);

CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

#### order_items テーブル

**主要な変更点**:
- `product_price INTEGER` → `product_price NUMERIC(10, 2)`
- `subtotal INTEGER` → `subtotal NUMERIC(10, 2)`

```sql
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_price NUMERIC(10, 2) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    subtotal NUMERIC(10, 2) NOT NULL CHECK (subtotal >= 0),

    -- 監査カラム
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_type VARCHAR(50),
    created_by_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_type VARCHAR(50),
    updated_by_id BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by BIGINT,

    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE RESTRICT
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_order_items_is_deleted ON order_items(is_deleted);
CREATE INDEX idx_order_items_created_by ON order_items(created_by_type, created_by_id);
CREATE INDEX idx_order_items_updated_by ON order_items(updated_by_type, updated_by_id);

CREATE TRIGGER update_order_items_updated_at
    BEFORE UPDATE ON order_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

#### bo_users テーブル

```sql
CREATE TABLE bo_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    permission_level VARCHAR(50) NOT NULL DEFAULT 'OPERATOR' CHECK (permission_level IN ('SUPER_ADMIN', 'ADMIN', 'OPERATOR')),
    last_login_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- 監査カラム
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_type VARCHAR(50),
    created_by_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_type VARCHAR(50),
    updated_by_id BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by_type VARCHAR(50),
    deleted_by_id BIGINT
);

CREATE INDEX idx_bo_users_email ON bo_users(email) WHERE is_deleted = FALSE;
CREATE INDEX idx_bo_users_is_active ON bo_users(is_active) WHERE is_deleted = FALSE;
CREATE INDEX idx_bo_users_is_deleted ON bo_users(is_deleted);
CREATE INDEX idx_bo_users_created_by ON bo_users(created_by_type, created_by_id);
CREATE INDEX idx_bo_users_updated_by ON bo_users(updated_by_type, updated_by_id);

CREATE TRIGGER update_bo_users_updated_at
    BEFORE UPDATE ON bo_users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

#### bo_auth_tokens テーブル

```sql
CREATE TABLE bo_auth_tokens (
    id BIGSERIAL PRIMARY KEY,
    bo_user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,

    -- 監査カラム
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_type VARCHAR(50),
    created_by_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_type VARCHAR(50),
    updated_by_id BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by BIGINT,

    CONSTRAINT fk_bo_auth_tokens_bo_user FOREIGN KEY (bo_user_id)
        REFERENCES bo_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_bo_auth_tokens_token_hash ON bo_auth_tokens(token_hash) WHERE is_deleted = FALSE;
CREATE INDEX idx_bo_auth_tokens_bo_user_id ON bo_auth_tokens(bo_user_id);
CREATE INDEX idx_bo_auth_tokens_expires_at ON bo_auth_tokens(expires_at);
CREATE INDEX idx_bo_auth_tokens_is_deleted ON bo_auth_tokens(is_deleted);
CREATE INDEX idx_bo_auth_tokens_created_by ON bo_auth_tokens(created_by_type, created_by_id);
CREATE INDEX idx_bo_auth_tokens_updated_by ON bo_auth_tokens(updated_by_type, updated_by_id);

CREATE TRIGGER update_bo_auth_tokens_updated_at
    BEFORE UPDATE ON bo_auth_tokens
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

#### inventory_adjustments テーブル

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

    -- 監査カラム
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_type VARCHAR(50),
    created_by_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_type VARCHAR(50),
    updated_by_id BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by BIGINT,

    CONSTRAINT fk_inventory_adjustments_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_inventory_adjustments_product_id ON inventory_adjustments(product_id);
CREATE INDEX idx_inventory_adjustments_adjusted_at ON inventory_adjustments(adjusted_at);
CREATE INDEX idx_inventory_adjustments_is_deleted ON inventory_adjustments(is_deleted);
CREATE INDEX idx_inventory_adjustments_created_by ON inventory_adjustments(created_by_type, created_by_id);
CREATE INDEX idx_inventory_adjustments_updated_by ON inventory_adjustments(updated_by_type, updated_by_id);

CREATE TRIGGER update_inventory_adjustments_updated_at
    BEFORE UPDATE ON inventory_adjustments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

#### operation_history テーブル

**主要な変更点**:
- `details TEXT` → `details VARCHAR(2000)`

```sql
CREATE TABLE operation_history (
    id BIGSERIAL PRIMARY KEY,
    operation_type VARCHAR(100) NOT NULL,
    performed_by VARCHAR(255),
    request_path VARCHAR(500),
    details VARCHAR(2000),

    -- 監査カラム（operation_history は論理削除しない）
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_type VARCHAR(50),
    created_by_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_type VARCHAR(50),
    updated_by_id BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by_type VARCHAR(50),
    deleted_by_id BIGINT
);

CREATE INDEX idx_operation_history_operation_type ON operation_history(operation_type);
CREATE INDEX idx_operation_history_performed_by ON operation_history(performed_by);
CREATE INDEX idx_operation_history_created_at ON operation_history(created_at);
CREATE INDEX idx_operation_history_is_deleted ON operation_history(is_deleted);
CREATE INDEX idx_operation_history_created_by ON operation_history(created_by_type, created_by_id);
CREATE INDEX idx_operation_history_updated_by ON operation_history(updated_by_type, updated_by_id);

CREATE TRIGGER update_operation_history_updated_at
    BEFORE UPDATE ON operation_history
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### 1.4 シーケンスの初期値設定（マイグレーション時）

データ移行後、シーケンスの現在値を最大 ID + 1 に設定する必要があります。

```sql
-- 例: users テーブルのシーケンス調整
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM users), false);
SELECT setval('products_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM products), false);
-- 以下、全テーブルに対して同様に実施
```

---

## 2. データ移行手順

### 2.1 移行方針

**アプローチ**: SQLite ダンプ → CSV エクスポート → PostgreSQL COPY

#### 移行手順の概要

1. SQLite からテーブルごとに CSV をエクスポート
2. PostgreSQL にテーブルを作成（スキーマ定義実行）
3. CSV を PostgreSQL に COPY コマンドでインポート
4. シーケンスの初期値を調整
5. データ整合性検証（件数照合、外部キー制約確認）

### 2.2 SQLite からのデータエクスポート

**スクリプト**: `scripts/export_sqlite_to_csv.sh`

```bash
#!/bin/bash

SQLITE_DB="backend/data/ec.db"
OUTPUT_DIR="migration/csv"

mkdir -p "$OUTPUT_DIR"

TABLES=(
  "users"
  "auth_tokens"
  "products"
  "reservations"
  "orders"
  "order_items"
  "bo_users"
  "bo_auth_tokens"
  "inventory_adjustments"
  "operation_history"
)

for table in "${TABLES[@]}"; do
  echo "Exporting $table..."
  sqlite3 "$SQLITE_DB" <<EOF
.headers on
.mode csv
.output $OUTPUT_DIR/$table.csv
SELECT * FROM $table;
.quit
EOF
done

echo "Export completed to $OUTPUT_DIR"
```

### 2.3 PostgreSQL へのデータインポート

**スクリプト**: `scripts/import_csv_to_postgres.sh`

```bash
#!/bin/bash

PGHOST="localhost"
PGPORT="5432"
PGDATABASE="ec_app"
PGUSER="ec_app_user"
PGPASSWORD="changeme"
CSV_DIR="migration/csv"

export PGPASSWORD

TABLES=(
  "users"
  "auth_tokens"
  "products"
  "reservations"
  "orders"
  "order_items"
  "bo_users"
  "bo_auth_tokens"
  "inventory_adjustments"
  "operation_history"
)

for table in "${TABLES[@]}"; do
  echo "Importing $table..."
  psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c \
    "\COPY $table FROM '$CSV_DIR/$table.csv' WITH (FORMAT csv, HEADER true, DELIMITER ',', QUOTE '\"', ESCAPE '\"');"
done

echo "Import completed"
```

### 2.4 シーケンス調整スクリプト

**スクリプト**: `scripts/adjust_sequences.sql`

```sql
-- users
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM users), false);

-- auth_tokens
SELECT setval('auth_tokens_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM auth_tokens), false);

-- products
SELECT setval('products_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM products), false);

-- reservations
SELECT setval('reservations_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM reservations), false);

-- orders
SELECT setval('orders_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM orders), false);

-- order_items
SELECT setval('order_items_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM order_items), false);

-- bo_users
SELECT setval('bo_users_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM bo_users), false);

-- bo_auth_tokens
SELECT setval('bo_auth_tokens_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM bo_auth_tokens), false);

-- inventory_adjustments
SELECT setval('inventory_adjustments_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM inventory_adjustments), false);

-- operation_history
SELECT setval('operation_history_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM operation_history), false);
```

### 2.5 データ整合性検証

**スクリプト**: `scripts/verify_migration.sql`

```sql
-- 件数照合
SELECT 'users' AS table_name, COUNT(*) AS count FROM users
UNION ALL
SELECT 'auth_tokens', COUNT(*) FROM auth_tokens
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'reservations', COUNT(*) FROM reservations
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL
SELECT 'bo_users', COUNT(*) FROM bo_users
UNION ALL
SELECT 'bo_auth_tokens', COUNT(*) FROM bo_auth_tokens
UNION ALL
SELECT 'inventory_adjustments', COUNT(*) FROM inventory_adjustments
UNION ALL
SELECT 'operation_history', COUNT(*) FROM operation_history;

-- 外部キー制約の検証（孤立レコードチェック）
SELECT 'auth_tokens orphans' AS check_name, COUNT(*) AS count
FROM auth_tokens t
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = t.user_id);

SELECT 'reservations orphans (product)' AS check_name, COUNT(*) AS count
FROM reservations r
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.id = r.product_id);

SELECT 'order_items orphans (order)' AS check_name, COUNT(*) AS count
FROM order_items oi
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.id = oi.order_id);

SELECT 'order_items orphans (product)' AS check_name, COUNT(*) AS count
FROM order_items oi
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.id = oi.product_id);
```

---

## 3. アプリケーション設定変更

### 3.1 pom.xml（依存関係の変更）

**削除する依存関係**:
```xml
<!-- SQLite 削除 -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-community-dialects</artifactId>
</dependency>
```

**追加する依存関係**:
```xml
<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 3.2 application.yml（データソース設定）

**変更前（SQLite）**:
```yaml
spring:
  datasource:
    url: jdbc:sqlite:${DB_PATH:./data/ec.db}
    driver-class-name: org.sqlite.JDBC
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: update
```

**変更後（PostgreSQL）**:
```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/ec_app}
    username: ${DB_USER:ec_app_user}
    password: ${DB_PASSWORD:changeme}
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # 本番は validate、開発は update でも可
    properties:
      hibernate:
        format_sql: true
        show_sql: false
```

### 3.3 環境変数設定

**開発環境** (`.env` または環境変数):
```bash
DB_URL=jdbc:postgresql://localhost:5432/ec_app
DB_USER=ec_app_user
DB_PASSWORD=changeme
```

**本番環境**:
```bash
DB_URL=jdbc:postgresql://production-db-host:5432/ec_app
DB_USER=ec_app_user
DB_PASSWORD=<secure-password>
```

---

## 4. トランザクション・排他制御の見直し

### 4.1 @Transactional の設定

**全ての @Transactional に以下を適用**:

```java
@Transactional(rollbackFor = Exception.class)
```

**理由**: デフォルトでは RuntimeException のみロールバック。チェック例外でもロールバックするため明示的に指定。

---

### 4.2 在庫引当の排他制御（行ロック）

**変更方針**: SQLite ではデータベース全体ロックだったが、PostgreSQL では行レベルロックを使用。

#### 在庫更新時の排他ロック

**ファイル**: `backend/src/main/java/com/example/aiec/service/InventoryService.java`

**変更箇所**: `reserveTentative()` メソッド

```java
@Transactional(rollbackFor = Exception.class)
public void reserveTentative(Long productId, int quantity, String sessionId, Long userId) {
    // 1. 商品を排他ロック（SELECT ... FOR UPDATE）
    Product product = productRepository.findByIdForUpdate(productId)
        .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "商品が見つかりません"));

    // 2. 在庫チェック
    int available = calculateAvailableStock(productId);
    if (available < quantity) {
        throw new BusinessException("INSUFFICIENT_STOCK", "在庫が不足しています");
    }

    // 3. 仮引当レコード作成
    Reservation reservation = new Reservation();
    reservation.setProductId(productId);
    reservation.setQuantity(quantity);
    reservation.setSessionId(sessionId);
    reservation.setUserId(userId);
    reservation.setReservationType(ReservationType.TENTATIVE);
    reservation.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
    reservationRepository.save(reservation);
}
```

**ProductRepository に追加**:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdForUpdate(@Param("id") Long id);
```

**説明**:
- `LockModeType.PESSIMISTIC_WRITE`: PostgreSQL の `SELECT ... FOR UPDATE` に相当
- 他のトランザクションは同じ商品の更新を待つことになる

### 4.3 トランザクション分離レベル

**デフォルト**: `READ COMMITTED`（PostgreSQL のデフォルト）

**在庫系処理**: `REPEATABLE READ` を使用（競合時の整合性確保）

**設定方法**:
```java
@Transactional(
    rollbackFor = Exception.class,
    isolation = Isolation.REPEATABLE_READ
)
public void reserveTentative(...) {
    // ...
}
```

**推奨**: 在庫引当・注文確定など整合性が重要な処理は `REPEATABLE READ` を使用。

---

### 4.4 デッドロック対策

**方針**:
1. **ロック順序の統一**: 複数テーブルをロックする場合、常に同じ順序でロックする（例: products → reservations → orders）
2. **タイムアウト設定**: ロック待機時間の上限を設定
3. **リトライロジック**: デッドロック検出時に自動リトライ

**application.yml にタイムアウト設定**:
```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 30000  # 30秒
      maximum-pool-size: 10
```

---

### 4.5 論理削除の実装

**方針**: 全テーブルで物理削除を禁止し、論理削除（is_deleted = TRUE）を使用。

#### エンティティの @SQLDelete と @Where

**例: Product エンティティ**:

```java
@Entity
@Table(name = "products")
@SQLDelete(sql = "UPDATE products SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "is_deleted = FALSE")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 2000)
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private Integer stock;
    private String image;
    private Boolean isPublished;

    // 監査カラム
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by_type", length = 50)
    private ActorType createdByType;

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "updated_by_type", length = 50)
    private ActorType updatedByType;

    @Column(name = "updated_by_id")
    private Long updatedById;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "deleted_by_type", length = 50)
    private ActorType deletedByType;

    @Column(name = "deleted_by_id")
    private Long deletedById;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        isDeleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

**説明**:
- `@SQLDelete`: DELETE 文を UPDATE に置き換える
- `@Where`: SELECT 時に `is_deleted = FALSE` を自動で付与
- `@PrePersist`, `@PreUpdate`: 日時の自動設定

#### サービス層での論理削除

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id, Long deletedBy) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "商品が見つかりません"));

        // deleted_by を設定してから delete() を呼ぶ
        product.setDeletedBy(deletedBy);
        productRepository.delete(product); // @SQLDelete が実行される
    }
}
```

**注意**: `deleted_by` は @SQLDelete の SQL に含めるため、エンティティに設定してから delete() を呼ぶ必要があります。または、カスタムリポジトリメソッドを作成します。

#### カスタムリポジトリメソッド（推奨）

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying
    @Query("UPDATE Product p SET p.isDeleted = TRUE, p.deletedAt = CURRENT_TIMESTAMP, p.deletedByType = :deletedByType, p.deletedById = :deletedById WHERE p.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedByType") ActorType deletedByType, @Param("deletedById") Long deletedById);
}
```

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id, ActorType deletedByType, Long deletedById) {
        productRepository.softDelete(id, deletedByType, deletedById);
    }

    // 顧客が削除する場合
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductByUser(Long id, Long userId) {
        productRepository.softDelete(id, ActorType.USER, userId);
    }

    // 管理者が削除する場合
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductByBoUser(Long id, Long boUserId) {
        productRepository.softDelete(id, ActorType.BO_USER, boUserId);
    }
}
```

---

## 5. Docker Compose 設定

### 5.1 docker-compose.yml（PostgreSQL 追加）

**ファイル**: `docker-compose.yml`

```yaml
version: '3.8'

services:
  # PostgreSQL
  postgres:
    image: postgres:16-alpine
    container_name: ec-postgres
    environment:
      POSTGRES_DB: ec_app
      POSTGRES_USER: ec_app_user
      POSTGRES_PASSWORD: changeme
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8 --lc-collate=ja_JP.UTF-8 --lc-ctype=ja_JP.UTF-8"
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backend/src/main/resources/db/init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ec_app_user -d ec_app"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Backend
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: ec-backend
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/ec_app
      DB_USER: ec_app_user
      DB_PASSWORD: changeme
    depends_on:
      postgres:
        condition: service_healthy
    volumes:
      - ./backend:/app

  # Frontend
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: ec-frontend
    ports:
      - "5173:5173"
    environment:
      VITE_API_URL: http://localhost:8080
    volumes:
      - ./frontend:/app
      - /app/node_modules

volumes:
  postgres_data:
```

### 5.2 初期化スクリプト

**ディレクトリ**: `backend/src/main/resources/db/init/`

**ファイル**: `01_create_schema.sql`（前述のスキーマ定義をコピー）

**ファイル**: `02_insert_sample_data.sql`（サンプルデータ投入）

```sql
-- サンプル商品
INSERT INTO products (name, description, price, stock, image, is_published) VALUES
('商品A', '説明A', 1000, 100, '/images/product-a.jpg', true),
('商品B', '説明B', 2000, 50, '/images/product-b.jpg', true);

-- サンプル管理者
INSERT INTO bo_users (email, password_hash, display_name, permission_level, is_active) VALUES
('admin@example.com', '$2a$10$...', '管理者', 'SUPER_ADMIN', true);
```

---

## 6. 運用手順

### 6.1 バックアップ手順

#### 論理バックアップ（pg_dump）

```bash
#!/bin/bash
# scripts/backup_postgres.sh

BACKUP_DIR="backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/ec_app_$TIMESTAMP.sql"

mkdir -p "$BACKUP_DIR"

pg_dump -h localhost -p 5432 -U ec_app_user -d ec_app -F c -f "$BACKUP_FILE"

echo "Backup completed: $BACKUP_FILE"

# 古いバックアップを削除（7日以上前）
find "$BACKUP_DIR" -name "*.sql" -mtime +7 -delete
```

#### 物理バックアップ（pg_basebackup）

```bash
#!/bin/bash
# scripts/physical_backup.sh

BACKUP_DIR="backups/physical"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

mkdir -p "$BACKUP_DIR/$TIMESTAMP"

pg_basebackup -h localhost -p 5432 -U ec_app_user -D "$BACKUP_DIR/$TIMESTAMP" -F tar -z -P

echo "Physical backup completed: $BACKUP_DIR/$TIMESTAMP"
```

### 6.2 リストア手順

```bash
#!/bin/bash
# scripts/restore_postgres.sh

BACKUP_FILE="$1"

if [ -z "$BACKUP_FILE" ]; then
  echo "Usage: $0 <backup_file>"
  exit 1
fi

# データベースを再作成
psql -h localhost -p 5432 -U postgres -c "DROP DATABASE IF EXISTS ec_app;"
psql -h localhost -p 5432 -U postgres -c "CREATE DATABASE ec_app OWNER ec_app_user;"

# リストア
pg_restore -h localhost -p 5432 -U ec_app_user -d ec_app -v "$BACKUP_FILE"

echo "Restore completed from: $BACKUP_FILE"
```

### 6.3 ロールバック手順

**前提**: 移行前に SQLite のバックアップを取得済み

```bash
#!/bin/bash
# scripts/rollback_to_sqlite.sh

# 1. アプリケーション停止
docker compose down

# 2. application.yml を SQLite 設定に戻す
# （Git で管理している場合は git checkout）

# 3. SQLite バックアップから復元
cp backups/ec.db.backup backend/data/ec.db

# 4. アプリケーション起動（SQLite モード）
docker compose up -d

echo "Rollback to SQLite completed"
```

---

## 7. 性能最適化

### 7.1 接続プール設定（HikariCP）

**application.yml**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10       # 最大接続数
      minimum-idle: 5             # 最小アイドル接続数
      connection-timeout: 30000   # 接続タイムアウト（30秒）
      idle-timeout: 600000        # アイドルタイムアウト（10分）
      max-lifetime: 1800000       # 最大ライフタイム（30分）
```

### 7.2 クエリ最適化

#### EXPLAIN ANALYZE の活用

```sql
-- 実行計画の確認
EXPLAIN ANALYZE
SELECT * FROM products WHERE is_published = true ORDER BY created_at DESC LIMIT 10;
```

#### インデックスの追加検討

```sql
-- 複合インデックス（よく使われる検索条件）
CREATE INDEX idx_products_published_created ON products(is_published, created_at DESC);

-- 部分インデックス（条件付きインデックス）
CREATE INDEX idx_products_published_only ON products(created_at DESC) WHERE is_published = true;
```

### 7.3 スロークエリのログ記録

**PostgreSQL 設定** (`postgresql.conf` または Docker 環境変数):
```
log_min_duration_statement = 1000  # 1秒以上かかるクエリをログ記録
```

---

## 8. 監視・ヘルスチェック

### 8.1 接続数監視

```sql
-- 現在の接続数
SELECT count(*) FROM pg_stat_activity WHERE datname = 'ec_app';

-- 接続状態の詳細
SELECT pid, usename, application_name, state, query
FROM pg_stat_activity
WHERE datname = 'ec_app';
```

### 8.2 ロック状況の監視

```sql
-- ロック待ちの確認
SELECT
    blocked_locks.pid AS blocked_pid,
    blocked_activity.usename AS blocked_user,
    blocking_locks.pid AS blocking_pid,
    blocking_activity.usename AS blocking_user,
    blocked_activity.query AS blocked_statement,
    blocking_activity.query AS blocking_statement
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks
    ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
    AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
    AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
    AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
    AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
    AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
    AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
    AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
    AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
    AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

### 8.3 ヘルスチェックエンドポイント

**Spring Boot Actuator** を使用:

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**application.yml**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

**アクセス**:
```bash
curl http://localhost:8080/actuator/health
```

---

## 9. テスト計画

### 9.1 移行リハーサル

**手順**:
1. SQLite ダンプ取得
2. PostgreSQL スキーマ作成
3. データ移行スクリプト実行
4. 件数照合（SQLite vs PostgreSQL）
5. 外部キー制約確認
6. アプリケーション起動確認

### 9.2 回帰テスト

**対象**:
- 顧客向け API（商品一覧、カート追加、注文確定、注文履歴）
- 管理 API（商品管理、注文管理、在庫管理、会員管理）

**確認項目**:
- レスポンス形式が変わっていないこと
- エラーコードが変わっていないこと
- 応答時間が著しく悪化していないこと

### 9.3 競合テスト

**シナリオ**:
1. 同一商品に対する同時注文（10 並列）
2. 在庫更新と注文処理の同時実行
3. 同一商品の在庫調整の同時実行

**確認項目**:
- 在庫がマイナスにならないこと
- デッドロックが発生しても適切にリトライされること
- データ整合性が維持されること

### 9.4 性能テスト

**ツール**: Apache JMeter または Gatling

**シナリオ**:
- 商品一覧取得（100 req/sec × 1分）
- 注文確定（10 req/sec × 5分）
- 管理画面の在庫一覧（10 req/sec × 1分）

**測定項目**:
- 平均応答時間
- 95 パーセンタイル応答時間
- エラー率

---

## 10. まとめ

### 主要な変更ポイント

1. **データベース**: SQLite → PostgreSQL 16
2. **型変更**:
   - `DATETIME` → `TIMESTAMP WITH TIME ZONE`（タイムゾーン対応）
   - `INTEGER` → `NUMERIC(10, 2)`（金額の小数点対応）
   - `AUTOINCREMENT` → `BIGSERIAL`（64bit ID）
   - `TEXT` → `VARCHAR(2000)`（適切な上限）
3. **監査カラム**: 全テーブルに以下を追加
   - `created_at`, `created_by_type`, `created_by_id`（登録日時・種別・ID）
   - `updated_at`, `updated_by_type`, `updated_by_id`（更新日時・種別・ID）
   - `is_deleted`, `deleted_at`, `deleted_by_type`, `deleted_by_id`（削除フラグ・日時・種別・ID）
   - ActorType: `USER` (顧客), `BO_USER` (管理者), `SYSTEM` (システム)
4. **論理削除**: 全テーブルで物理削除を禁止、`is_deleted = TRUE` で論理削除
5. **排他制御**: データベース全体ロック → 行レベルロック（`SELECT FOR UPDATE`）
6. **トランザクション**: `@Transactional(rollbackFor = Exception.class)` を全サービスに適用
7. **Java型変更**:
   - `LocalDateTime` → `Instant`（UTC統一）
   - `Integer price` → `BigDecimal price`（小数点対応）
8. **接続管理**: HikariCP による接続プール
9. **運用**: バックアップ・リストア・監視手順の整備

### 実装時の注意事項

1. **エンティティ**: 全エンティティに `@SQLDelete` と `@Where` を追加
2. **リポジトリ**: カスタム論理削除メソッド（`softDelete`）を実装
3. **サービス**: 削除処理は全て `softDelete` を使用
4. **マイグレーション**: 既存データの監査カラムは NULL または デフォルト値で初期化
5. **テスト**: 論理削除後のデータが検索結果に含まれないことを確認

### 次のステップ

技術設計が完了したので、次は実装タスク（CHG-009）の作成に進みます。
