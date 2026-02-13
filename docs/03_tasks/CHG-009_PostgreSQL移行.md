# CHG-009: PostgreSQL移行 - 実装タスク

## 検証コマンド

```bash
# PostgreSQL起動確認
docker compose up -d postgres
docker compose ps
docker compose logs postgres

# バックエンド起動（PostgreSQL接続）
cd backend
./mvnw spring-boot:run

# データベース接続確認
psql -h localhost -p 5432 -U ec_app_user -d ec_app

# テーブル確認
\dt
\d users

# API動作確認
curl http://localhost:8080/api/item
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","displayName":"テスト","password":"password123"}'
```

---

## 📌 実装の前提条件

**重要**: CHG-009は **CHG-008（ドメイン分離とBoUser管理）完了後** に実施します。

### CHG-008で既に実装済みの内容

- ✅ `bo_users` テーブル（SQLite版）
- ✅ `bo_auth_tokens` テーブル（SQLite版）
- ✅ `BoUser`, `BoAuthToken` エンティティ（LocalDateTime使用）
- ✅ `Role` enum（後でActorTypeに置き換え）
- ✅ BoAuth API（`/api/bo-auth/**`）
- ✅ 管理API（`/api/bo/admin/**`）

### CHG-009で実施する内容

1. **Phase 1**: PostgreSQL環境整備 + スキーマ作成（全テーブル + 監査カラム）
2. **Phase 2**: アプリケーション設定変更（SQLite → PostgreSQL）
3. **Phase 3**: エンティティ修正（LocalDateTime→Instant、監査カラム、論理削除）
4. **Phase 4**: データ移行 + テスト

### 主要な変更点

- `LocalDateTime` → `Instant`（全エンティティ）
- `Integer price` → `BigDecimal price`（Product, Order, OrderItem）
- 監査カラム追加（`created_by_type/id`, `updated_by_type/id`, `deleted_by_type/id`）
- `Role` enum → `ActorType` enum に置き換え
- 論理削除の実装（`@SQLDelete`, `@Where`）

---

## Phase 1: PostgreSQL環境整備 + スキーマ作成

### Task 1-1: docker-compose.yml にPostgreSQLを追加

**ファイル**: `docker-compose.yml`（既存ファイル）

**挿入位置**: services セクションの先頭

```yaml
services:
  # PostgreSQL
  postgres:
    image: postgres:16-alpine
    container_name: ec-postgres
    environment:
      POSTGRES_DB: ec_app
      POSTGRES_USER: ec_app_user
      POSTGRES_PASSWORD: changeme
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8 --lc-collate=C --lc-ctype=C"
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

  # Backend（既存サービスを修正）
  backend:
    # ... 既存の設定 ...
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/ec_app
      DB_USER: ec_app_user
      DB_PASSWORD: changeme
    depends_on:
      postgres:
        condition: service_healthy
```

**volumes セクションに追加**:

```yaml
volumes:
  postgres_data:
```

**検証**:
```bash
docker compose up -d postgres
docker compose ps
# → postgres が healthy になることを確認
```

---

### Task 1-2: PostgreSQLスキーマ定義ファイル作成

**ディレクトリ**: `backend/src/main/resources/db/init/`（新規作成）

**ファイル**: `01_create_schema.sql`（新規作成）

**参考**: `docs/02_designs/CHG-009_PostgreSQL移行.md` のスキーマ定義

```sql
-- ActorType用のCHECK制約を共通化するための定数定義（コメント）
-- ActorType: 'USER', 'BO_USER', 'SYSTEM'

-- 共通トリガー関数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- users テーブル
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

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- auth_tokens テーブル
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

-- products テーブル
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
    deleted_by_type VARCHAR(50),
    deleted_by_id BIGINT
);

CREATE INDEX idx_products_is_published ON products(is_published) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_is_deleted ON products(is_deleted);
CREATE INDEX idx_products_created_by ON products(created_by_type, created_by_id);
CREATE INDEX idx_products_updated_by ON products(updated_by_type, updated_by_id);

CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- reservations テーブル
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
    deleted_by_type VARCHAR(50),
    deleted_by_id BIGINT,

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

-- orders テーブル
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
    deleted_by_type VARCHAR(50),
    deleted_by_id BIGINT,

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

-- order_items テーブル
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
    deleted_by_type VARCHAR(50),
    deleted_by_id BIGINT,

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

-- operation_history テーブル
CREATE TABLE operation_history (
    id BIGSERIAL PRIMARY KEY,
    operation_type VARCHAR(100) NOT NULL,
    performed_by VARCHAR(255),
    request_path VARCHAR(500),
    details VARCHAR(2000),

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

-- bo_users テーブル（CHG-008で作成済みのテーブルをPostgreSQL化）
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

-- bo_auth_tokens テーブル（CHG-008で作成済みのテーブルをPostgreSQL化）
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
    deleted_by_type VARCHAR(50),
    deleted_by_id BIGINT,

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

-- inventory_adjustments テーブル（CHG-008で作成済みのテーブルをPostgreSQL化）
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
    deleted_by_type VARCHAR(50),
    deleted_by_id BIGINT,

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

**検証**:
```bash
docker compose restart postgres
docker compose logs postgres
psql -h localhost -p 5432 -U ec_app_user -d ec_app -c "\dt"
```

---

### Task 1-3: サンプルデータ投入スクリプト作成

**ファイル**: `backend/src/main/resources/db/init/02_insert_sample_data.sql`（新規作成）

```sql
-- サンプル商品（created_by_type = 'SYSTEM'）
INSERT INTO products (name, description, price, stock, image, is_published, created_by_type) VALUES
('エレガントレザーバッグ', '上質な本革を使用した高級バッグ', 45000.00, 20, '/images/bag-1.jpg', true, 'SYSTEM'),
('モダンウォッチ', 'シンプルで洗練されたデザインの腕時計', 28000.00, 15, '/images/watch-1.jpg', true, 'SYSTEM'),
('シルクスカーフ', 'イタリア製の高級シルクスカーフ', 12000.00, 30, '/images/scarf-1.jpg', true, 'SYSTEM'),
('レザーウォレット', '職人手作りの本革財布', 18000.00, 25, '/images/wallet-1.jpg', true, 'SYSTEM'),
('デザイナーサングラス', 'UV400保護レンズ搭載', 32000.00, 10, '/images/sunglasses-1.jpg', true, 'SYSTEM');
```

---

## Phase 2: アプリケーション設定変更

### Task 2-1: pom.xml の依存関係変更

**ファイル**: `backend/pom.xml`（既存ファイル）

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

**検証**:
```bash
./mvnw dependency:tree | grep postgres
```

---

### Task 2-2: application.yml の設定変更

**ファイル**: `backend/src/main/resources/application.yml`（既存ファイル）

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
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # 本番は validate
    properties:
      hibernate:
        format_sql: true
        show_sql: false
```

**検証**:
```bash
./mvnw spring-boot:run
# → 起動成功することを確認
```

---

## Phase 3: エンティティ修正（監査カラム + 論理削除）

### Task 3-1: ActorType enum 作成

**ファイル**: `backend/src/main/java/com/example/aiec/entity/ActorType.java`（新規作成）

```java
package com.example.aiec.entity;

public enum ActorType {
    USER,      // 顧客ユーザー (users.id)
    BO_USER,   // 管理者ユーザー (bo_users.id)
    SYSTEM     // システム（自動処理）
}
```

---

### Task 3-2: User エンティティの修正

**ファイル**: `backend/src/main/java/com/example/aiec/entity/User.java`（既存ファイル）

**変更内容**:
1. `LocalDateTime` → `Instant`
2. 監査カラム追加（created_by_type/id, updated_by_type/id, deleted_by_type/id）
3. `@SQLDelete` と `@Where` を追加（論理削除）

**変更後**:

```java
package com.example.aiec.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "is_deleted = FALSE")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
        if (createdByType == null) {
            createdByType = ActorType.SYSTEM;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

**同様の修正を以下のエンティティにも適用**:
- `Product.java`
- `AuthToken.java`
- `Reservation.java`
- `Order.java`
- `OrderItem.java`
- `OperationHistory.java`

**注意**: Product エンティティは `price` を `Integer` → `BigDecimal` に変更

```java
@Column(precision = 10, scale = 2)
private BigDecimal price;
```

---

### Task 3-3: リポジトリに論理削除メソッド追加

**ファイル**: `backend/src/main/java/com/example/aiec/repository/UserRepository.java`（既存ファイル）

**追加メソッド**:

```java
@Modifying
@Query("UPDATE User u SET u.isDeleted = TRUE, u.deletedAt = CURRENT_TIMESTAMP, u.deletedByType = :deletedByType, u.deletedById = :deletedById WHERE u.id = :id")
void softDelete(@Param("id") Long id, @Param("deletedByType") ActorType deletedByType, @Param("deletedById") Long deletedById);
```

**同様のメソッドを以下のリポジトリにも追加**:
- `ProductRepository.java`
- `AuthTokenRepository.java`
- `ReservationRepository.java`
- `OrderRepository.java`
- `OrderItemRepository.java`

---

### Task 3-4: サービス層の @Transactional 修正

**全ての @Transactional に rollbackFor を追加**:

```java
// 変更前
@Transactional
public void someMethod() { }

// 変更後
@Transactional(rollbackFor = Exception.class)
public void someMethod() { }
```

**対象ファイル**:
- `UserService.java`
- `ProductService.java`
- `AuthService.java`
- `CartService.java`
- `OrderService.java`
- `InventoryService.java`

---

### Task 3-5: 悲観的ロック（SELECT FOR UPDATE）の追加

**ファイル**: `backend/src/main/java/com/example/aiec/repository/ProductRepository.java`

**追加メソッド**:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdForUpdate(@Param("id") Long id);
```

**InventoryService での使用**:

```java
@Transactional(
    rollbackFor = Exception.class,
    isolation = Isolation.REPEATABLE_READ
)
public void reserveTentative(Long productId, int quantity, String sessionId, Long userId) {
    // 悲観的ロックで商品を取得
    Product product = productRepository.findByIdForUpdate(productId)
        .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "商品が見つかりません"));

    // 在庫チェック
    int available = calculateAvailableStock(productId);
    if (available < quantity) {
        throw new BusinessException("INSUFFICIENT_STOCK", "在庫が不足しています");
    }

    // 仮引当作成
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

---

## Phase 4: データ移行 + テスト

### Task 4-1: SQLiteからデータエクスポート（既存データがある場合）

**スクリプト**: `scripts/export_sqlite_to_csv.sh`（新規作成）

```bash
#!/bin/bash

SQLITE_DB="backend/data/ec.db"
OUTPUT_DIR="migration/csv"

# 既存データがない場合はスキップ
if [ ! -f "$SQLITE_DB" ]; then
    echo "SQLite database not found. Skipping export."
    exit 0
fi

mkdir -p "$OUTPUT_DIR"

TABLES=(
  "users"
  "auth_tokens"
  "products"
  "reservations"
  "orders"
  "order_items"
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

**実行**:
```bash
chmod +x scripts/export_sqlite_to_csv.sh
./scripts/export_sqlite_to_csv.sh
```

---

### Task 4-2: PostgreSQLへデータインポート（既存データがある場合）

**スクリプト**: `scripts/import_csv_to_postgres.sh`（新規作成）

```bash
#!/bin/bash

PGHOST="localhost"
PGPORT="5432"
PGDATABASE="ec_app"
PGUSER="ec_app_user"
PGPASSWORD="changeme"
CSV_DIR="migration/csv"

export PGPASSWORD

# CSVディレクトリが存在しない場合はスキップ
if [ ! -d "$CSV_DIR" ]; then
    echo "CSV directory not found. Skipping import."
    exit 0
fi

TABLES=(
  "users"
  "auth_tokens"
  "products"
  "reservations"
  "orders"
  "order_items"
  "operation_history"
)

for table in "${TABLES[@]}"; do
  if [ -f "$CSV_DIR/$table.csv" ]; then
    echo "Importing $table..."
    psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c \
      "\COPY $table FROM '$CSV_DIR/$table.csv' WITH (FORMAT csv, HEADER true, DELIMITER ',', QUOTE '\"', ESCAPE '\"');"
  fi
done

echo "Import completed"
```

**実行**:
```bash
chmod +x scripts/import_csv_to_postgres.sh
./scripts/import_csv_to_postgres.sh
```

---

### Task 4-3: シーケンス調整

**スクリプト**: `scripts/adjust_sequences.sql`（新規作成）

```sql
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM users), false);
SELECT setval('auth_tokens_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM auth_tokens), false);
SELECT setval('products_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM products), false);
SELECT setval('reservations_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM reservations), false);
SELECT setval('orders_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM orders), false);
SELECT setval('order_items_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM order_items), false);
SELECT setval('operation_history_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM operation_history), false);
```

**実行**:
```bash
psql -h localhost -p 5432 -U ec_app_user -d ec_app -f scripts/adjust_sequences.sql
```

---

### Task 4-4: 統合テスト

**テストシナリオ**:

1. **会員登録テスト**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","displayName":"テストユーザー","password":"password123"}'
```

2. **ログインテスト**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

3. **商品一覧取得テスト**
```bash
curl http://localhost:8080/api/item
```

4. **カート追加テスト**
```bash
curl -X POST http://localhost:8080/api/order/cart \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-session-123" \
  -d '{"productId":1,"quantity":2}'
```

5. **注文確定テスト**
```bash
curl -X POST http://localhost:8080/api/order/reg \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-session-123" \
  -d '{}'
```

6. **論理削除テスト（データベース確認）**
```sql
-- 商品を削除
-- サービス層で softDelete を呼ぶ

-- 削除確認
SELECT id, name, is_deleted, deleted_at FROM products;
-- → is_deleted = TRUE, deleted_at に日時が入っていることを確認

-- 検索結果に含まれないことを確認
SELECT * FROM products;
-- → @Where により削除済みレコードは除外される
```

---

## まとめ

### 実装完了チェックリスト

- [ ] Phase 1: PostgreSQL環境整備 + スキーマ作成（Task 1-1 〜 1-3）
- [ ] Phase 2: アプリケーション設定変更（Task 2-1 〜 2-2）
- [ ] Phase 3: エンティティ修正（Task 3-1 〜 3-5）
- [ ] Phase 4: データ移行 + テスト（Task 4-1 〜 4-4）

### 次のステップ

CHG-009完了後、CHG-008（ドメイン分離とBoUser管理）を実施します。
CHG-008では、CHG-009で整備した監査カラム（ActorType等）を活用してBoUserテーブルを追加します。
