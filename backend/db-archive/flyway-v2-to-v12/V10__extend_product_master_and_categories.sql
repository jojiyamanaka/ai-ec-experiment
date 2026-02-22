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
    ADD COLUMN sale_end_at TIMESTAMP WITH TIME ZONE;

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
    );

CREATE INDEX idx_products_product_code ON products(product_code) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_category_id ON products(category_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_publish_window ON products(publish_start_at, publish_end_at) WHERE is_deleted = FALSE;
CREATE INDEX idx_products_sale_window ON products(sale_start_at, sale_end_at) WHERE is_deleted = FALSE;
