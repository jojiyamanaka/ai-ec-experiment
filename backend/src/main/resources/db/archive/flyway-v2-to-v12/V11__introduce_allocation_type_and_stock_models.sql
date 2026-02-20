ALTER TABLE products
    ADD COLUMN allocation_type VARCHAR(20) NOT NULL DEFAULT 'REAL',
    ADD CONSTRAINT ck_products_allocation_type CHECK (allocation_type IN ('REAL', 'FRAME'));

ALTER TABLE order_items
    ADD COLUMN allocated_qty INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_order_items_allocated_qty_non_negative CHECK (allocated_qty >= 0),
    ADD CONSTRAINT ck_order_items_allocated_not_exceed_quantity CHECK (allocated_qty <= quantity);

CREATE TABLE location_stocks (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    location_id INTEGER NOT NULL DEFAULT 1,
    allocatable_qty INTEGER NOT NULL DEFAULT 0,
    allocated_qty INTEGER NOT NULL DEFAULT 0,

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
    CONSTRAINT ck_location_stocks_allocatable_qty_non_negative CHECK (allocatable_qty >= 0),
    CONSTRAINT ck_location_stocks_allocated_qty_non_negative CHECK (allocated_qty >= 0)
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
    sales_limit_total INTEGER NOT NULL DEFAULT 0,

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
    CONSTRAINT ck_sales_limits_total_non_negative CHECK (sales_limit_total >= 0)
);

CREATE INDEX idx_sales_limits_product_id ON sales_limits(product_id);
CREATE INDEX idx_sales_limits_is_deleted ON sales_limits(is_deleted);

CREATE TRIGGER update_sales_limits_updated_at
    BEFORE UPDATE ON sales_limits
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_orders_created_at_id ON orders(created_at, id) WHERE is_deleted = FALSE;
CREATE INDEX idx_order_items_order_id_id ON order_items(order_id, id) WHERE is_deleted = FALSE;
CREATE INDEX idx_order_items_product_allocated_qty ON order_items(product_id, allocated_qty, id) WHERE is_deleted = FALSE;

UPDATE products
SET allocation_type = 'REAL'
WHERE allocation_type IS NULL;

UPDATE products
SET stock = 0
WHERE stock <> 0;

INSERT INTO location_stocks (
    product_id,
    location_id,
    allocatable_qty,
    allocated_qty,
    created_by_type,
    updated_by_type
)
SELECT
    p.id,
    1,
    0,
    0,
    'SYSTEM',
    'SYSTEM'
FROM products p
WHERE NOT EXISTS (
    SELECT 1
    FROM location_stocks ls
    WHERE ls.product_id = p.id
      AND ls.location_id = 1
);

INSERT INTO sales_limits (
    product_id,
    sales_limit_total,
    created_by_type,
    updated_by_type
)
SELECT
    p.id,
    0,
    'SYSTEM',
    'SYSTEM'
FROM products p
WHERE NOT EXISTS (
    SELECT 1
    FROM sales_limits sl
    WHERE sl.product_id = p.id
);
