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
