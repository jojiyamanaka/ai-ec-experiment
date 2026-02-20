-- Latest schema snapshot
-- Source of truth for migration execution remains Flyway scripts under db/flyway.
-- This snapshot is refreshed when DB schema changes.

-- -----------------------------------------------------------------------------
-- 01) Base schema
-- -----------------------------------------------------------------------------

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

-- stock_reservations テーブル
CREATE TABLE stock_reservations (
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

    CONSTRAINT fk_stock_reservations_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_reservations_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_stock_reservations_product_id ON stock_reservations(product_id);
CREATE INDEX idx_stock_reservations_session_id ON stock_reservations(session_id);
CREATE INDEX idx_stock_reservations_user_id ON stock_reservations(user_id);
CREATE INDEX idx_stock_reservations_order_id ON stock_reservations(order_id);
CREATE INDEX idx_stock_reservations_type ON stock_reservations(reservation_type);
CREATE INDEX idx_stock_reservations_expires_at ON stock_reservations(expires_at);
CREATE INDEX idx_stock_reservations_is_deleted ON stock_reservations(is_deleted);
CREATE INDEX idx_stock_reservations_created_by ON stock_reservations(created_by_type, created_by_id);
CREATE INDEX idx_stock_reservations_updated_by ON stock_reservations(updated_by_type, updated_by_id);

CREATE TRIGGER update_stock_reservations_updated_at
    BEFORE UPDATE ON stock_reservations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- carts テーブル
CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_carts_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_carts_session_id ON carts(session_id);
CREATE INDEX idx_carts_user_id ON carts(user_id);
CREATE INDEX idx_carts_updated_at ON carts(updated_at);

CREATE TRIGGER update_carts_updated_at
    BEFORE UPDATE ON carts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- cart_items テーブル
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id)
        REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items(product_id);

CREATE TRIGGER update_cart_items_updated_at
    BEFORE UPDATE ON cart_items
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

-- operation_histories テーブル
CREATE TABLE operation_histories (
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

CREATE INDEX idx_operation_histories_operation_type ON operation_histories(operation_type);
CREATE INDEX idx_operation_histories_performed_by ON operation_histories(performed_by);
CREATE INDEX idx_operation_histories_created_at ON operation_histories(created_at);
CREATE INDEX idx_operation_histories_is_deleted ON operation_histories(is_deleted);
CREATE INDEX idx_operation_histories_created_by ON operation_histories(created_by_type, created_by_id);
CREATE INDEX idx_operation_histories_updated_by ON operation_histories(updated_by_type, updated_by_id);

CREATE TRIGGER update_operation_histories_updated_at
    BEFORE UPDATE ON operation_histories
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

-- -----------------------------------------------------------------------------
-- 02) Current baseline delta
-- -----------------------------------------------------------------------------

-- V2 baseline (current schema)
-- 注意: 旧 V2..V12 を archive 化し、最新スキーマ差分を1ファイルに集約する。

-- 注文番号採番シーケンス
CREATE SEQUENCE IF NOT EXISTS order_number_seq START 1;

DO $$
DECLARE
    status_constraint_name text;
BEGIN
    SELECT conname
      INTO status_constraint_name
      FROM pg_constraint
     WHERE conrelid = 'orders'::regclass
       AND contype = 'c'
       AND pg_get_constraintdef(oid) LIKE '%status%';

    IF status_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE orders DROP CONSTRAINT %I', status_constraint_name);
    END IF;
END $$;

ALTER TABLE orders
    ADD CONSTRAINT ck_orders_status
    CHECK (status IN ('PENDING', 'CONFIRMED', 'PREPARING_SHIPMENT', 'SHIPPED', 'DELIVERED', 'CANCELLED'));

ALTER TABLE users
    ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN terms_agreed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN full_name VARCHAR(100),
    ADD COLUMN phone_number VARCHAR(30),
    ADD COLUMN birth_date DATE,
    ADD COLUMN newsletter_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN member_rank VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN loyalty_points INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN deactivation_reason VARCHAR(500);

ALTER TABLE users
    ADD CONSTRAINT ck_users_member_rank
    CHECK (member_rank IN ('STANDARD', 'SILVER', 'GOLD', 'PLATINUM'));

ALTER TABLE users
    ADD CONSTRAINT ck_users_loyalty_points_non_negative
    CHECK (loyalty_points >= 0);

CREATE INDEX idx_users_member_rank ON users(member_rank) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_loyalty_points ON users(loyalty_points) WHERE is_deleted = FALSE;

CREATE TABLE user_addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label VARCHAR(100),
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone_number VARCHAR(30),
    postal_code VARCHAR(20) NOT NULL,
    prefecture VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    address_order INTEGER NOT NULL DEFAULT 0,

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

    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_user_addresses_address_order_non_negative
        CHECK (address_order >= 0)
);

CREATE UNIQUE INDEX uk_user_addresses_user_default
    ON user_addresses(user_id)
    WHERE is_default = TRUE AND is_deleted = FALSE;

CREATE INDEX idx_user_addresses_user_id ON user_addresses(user_id);
CREATE INDEX idx_user_addresses_user_order ON user_addresses(user_id, address_order) WHERE is_deleted = FALSE;
CREATE INDEX idx_user_addresses_is_deleted ON user_addresses(is_deleted);

CREATE TRIGGER update_user_addresses_updated_at
    BEFORE UPDATE ON user_addresses
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'outbox_event_status') THEN
        CREATE TYPE outbox_event_status AS ENUM ('PENDING', 'PROCESSING', 'PROCESSED', 'DEAD');
    END IF;
END
$$;

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

CREATE TABLE job_run_history (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(100) NOT NULL UNIQUE,
    job_type VARCHAR(100) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED')),
    processed_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(2000),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_job_run_history_job_type_started_at ON job_run_history(job_type, started_at DESC);
CREATE INDEX idx_job_run_history_status ON job_run_history(status);

CREATE TABLE shipments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    shipment_type VARCHAR(50) NOT NULL CHECK (shipment_type IN ('OUTBOUND', 'RETURN')),
    status VARCHAR(50) NOT NULL CHECK (status IN ('READY', 'EXPORTED', 'TRANSFERRED')),
    export_file_path VARCHAR(500),

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

    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT uk_shipments_order_type UNIQUE (order_id, shipment_type)
);

CREATE INDEX idx_shipments_status ON shipments(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_shipments_order_id ON shipments(order_id);
CREATE INDEX idx_shipments_export_file_path ON shipments(export_file_path) WHERE is_deleted = FALSE;
CREATE INDEX idx_shipments_is_deleted ON shipments(is_deleted);

CREATE TRIGGER update_shipments_updated_at
    BEFORE UPDATE ON shipments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE shipment_items (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_price NUMERIC(10, 2) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    subtotal NUMERIC(10, 2) NOT NULL CHECK (subtotal >= 0),

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

    CONSTRAINT fk_shipment_items_shipment FOREIGN KEY (shipment_id)
        REFERENCES shipments(id) ON DELETE CASCADE,
    CONSTRAINT fk_shipment_items_order_item FOREIGN KEY (order_item_id)
        REFERENCES order_items(id) ON DELETE RESTRICT
);

CREATE INDEX idx_shipment_items_shipment_id ON shipment_items(shipment_id);
CREATE INDEX idx_shipment_items_order_item_id ON shipment_items(order_item_id);
CREATE INDEX idx_shipment_items_is_deleted ON shipment_items(is_deleted);

CREATE TRIGGER update_shipment_items_updated_at
    BEFORE UPDATE ON shipment_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE product_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,

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

    CONSTRAINT uk_product_categories_name UNIQUE (name),
    CONSTRAINT ck_product_categories_display_order_non_negative CHECK (display_order >= 0)
);

CREATE INDEX idx_product_categories_is_published ON product_categories(is_published) WHERE is_deleted = FALSE;
CREATE INDEX idx_product_categories_display_order ON product_categories(display_order, id) WHERE is_deleted = FALSE;
CREATE INDEX idx_product_categories_is_deleted ON product_categories(is_deleted);

CREATE TRIGGER update_product_categories_updated_at
    BEFORE UPDATE ON product_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE products
    ADD COLUMN product_code VARCHAR(100),
    ADD COLUMN category_id BIGINT,
    ADD COLUMN publish_start_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN publish_end_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN sale_start_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN sale_end_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN allocation_type VARCHAR(20) NOT NULL DEFAULT 'REAL';

INSERT INTO product_categories (name, display_order, is_published, created_by_type, updated_by_type)
VALUES ('未分類', 0, TRUE, 'SYSTEM', 'SYSTEM')
ON CONFLICT (name) DO NOTHING;

UPDATE products
SET product_code = CONCAT('P', LPAD(id::text, 6, '0'))
WHERE product_code IS NULL;

UPDATE products
SET category_id = (
    SELECT id
    FROM product_categories
    WHERE name = '未分類'
    LIMIT 1
)
WHERE category_id IS NULL;

ALTER TABLE products
    ALTER COLUMN product_code SET NOT NULL,
    ALTER COLUMN category_id SET NOT NULL;

ALTER TABLE products
    ADD CONSTRAINT uk_products_product_code UNIQUE (product_code),
    ADD CONSTRAINT fk_products_category_id FOREIGN KEY (category_id) REFERENCES product_categories(id),
    ADD CONSTRAINT ck_products_price_integer CHECK (price = trunc(price)),
    ADD CONSTRAINT ck_products_publish_period CHECK (publish_start_at IS NULL OR publish_end_at IS NULL OR publish_start_at <= publish_end_at),
    ADD CONSTRAINT ck_products_sale_period CHECK (sale_start_at IS NULL OR sale_end_at IS NULL OR sale_start_at <= sale_end_at),
    ADD CONSTRAINT ck_products_sale_within_publish CHECK (
        (sale_start_at IS NULL OR publish_start_at IS NULL OR sale_start_at >= publish_start_at)
        AND (sale_end_at IS NULL OR publish_end_at IS NULL OR sale_end_at <= publish_end_at)
    ),
    ADD CONSTRAINT ck_products_allocation_type CHECK (allocation_type IN ('REAL', 'FRAME'));

CREATE INDEX idx_products_product_code ON products(product_code) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_category_id ON products(category_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_publish_window ON products(publish_start_at, publish_end_at) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_sale_window ON products(sale_start_at, sale_end_at) WHERE is_deleted = FALSE;

ALTER TABLE order_items
    ADD COLUMN committed_qty INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_order_items_committed_qty_non_negative CHECK (committed_qty >= 0),
    ADD CONSTRAINT ck_order_items_committed_not_exceed_quantity CHECK (committed_qty <= quantity);

CREATE TABLE location_stocks (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    location_id INTEGER NOT NULL DEFAULT 1,
    available_qty INTEGER NOT NULL DEFAULT 0,
    committed_qty INTEGER NOT NULL DEFAULT 0,

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

    CONSTRAINT fk_location_stocks_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_location_stocks_product_location UNIQUE (product_id, location_id),
    CONSTRAINT ck_location_stocks_available_qty_non_negative CHECK (available_qty >= 0),
    CONSTRAINT ck_location_stocks_committed_qty_non_negative CHECK (committed_qty >= 0)
);

CREATE INDEX idx_location_stocks_product_id ON location_stocks(product_id);
CREATE INDEX idx_location_stocks_location_id ON location_stocks(location_id);
CREATE INDEX idx_location_stocks_is_deleted ON location_stocks(is_deleted);

CREATE TRIGGER update_location_stocks_updated_at
    BEFORE UPDATE ON location_stocks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE sales_limits (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    frame_limit_qty INTEGER NOT NULL DEFAULT 0,

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

    CONSTRAINT fk_sales_limits_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_sales_limits_product UNIQUE (product_id),
    CONSTRAINT ck_sales_limits_frame_limit_qty_non_negative CHECK (frame_limit_qty >= 0)
);

CREATE INDEX idx_sales_limits_product_id ON sales_limits(product_id);
CREATE INDEX idx_sales_limits_is_deleted ON sales_limits(is_deleted);

CREATE TRIGGER update_sales_limits_updated_at
    BEFORE UPDATE ON sales_limits
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_orders_created_at_id ON orders(created_at, id) WHERE is_deleted = FALSE;
CREATE INDEX idx_order_items_order_id_id ON order_items(order_id, id) WHERE is_deleted = FALSE;
CREATE INDEX idx_order_items_product_committed_qty ON order_items(product_id, committed_qty, id) WHERE is_deleted = FALSE;

SELECT setval(
    'order_number_seq',
    COALESCE(
        (
            SELECT MAX(CAST(SUBSTRING(order_number FROM 5) AS BIGINT))
            FROM orders
            WHERE order_number ~ '^ORD-[0-9]{10}$'
        ),
        0
    ) + 1,
    false
);
