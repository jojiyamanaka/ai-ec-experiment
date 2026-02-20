-- E2E seed data (members, products, inventory)
-- Rule: update only impacted tables when DB schema changes.

-- 1) Members
INSERT INTO users (
    email,
    password_hash,
    display_name,
    full_name,
    is_active,
    member_rank,
    loyalty_points,
    created_by_type,
    updated_by_type
)
VALUES
    (
        'member01@example.com',
        '$2a$10$.er/VYSjKnsPJ4Lx4XLg2u5LS/htFxp2QdD/lC.BpozDocM1R2bgS',
        'E2E Member 01',
        'E2E Member 01',
        TRUE,
        'STANDARD',
        10,
        'SYSTEM',
        'SYSTEM'
    ),
    (
        'member02@example.com',
        '$2a$10$.er/VYSjKnsPJ4Lx4XLg2u5LS/htFxp2QdD/lC.BpozDocM1R2bgS',
        'E2E Member 02',
        'E2E Member 02',
        FALSE,
        'GOLD',
        120,
        'SYSTEM',
        'SYSTEM'
    )
ON CONFLICT (email) DO UPDATE
SET
    password_hash = EXCLUDED.password_hash,
    display_name = EXCLUDED.display_name,
    full_name = EXCLUDED.full_name,
    is_active = EXCLUDED.is_active,
    member_rank = EXCLUDED.member_rank,
    loyalty_points = EXCLUDED.loyalty_points,
    updated_by_type = 'SYSTEM',
    is_deleted = FALSE,
    deleted_at = NULL,
    deleted_by_type = NULL,
    deleted_by_id = NULL;

INSERT INTO bo_users (
    email,
    password_hash,
    display_name,
    permission_level,
    is_active,
    created_by_type,
    updated_by_type
)
VALUES (
    'admin@example.com',
    '$2a$10$.er/VYSjKnsPJ4Lx4XLg2u5LS/htFxp2QdD/lC.BpozDocM1R2bgS',
    'admin',
    'ADMIN',
    TRUE,
    'SYSTEM',
    'SYSTEM'
)
ON CONFLICT (email) DO UPDATE
SET
    password_hash = EXCLUDED.password_hash,
    display_name = EXCLUDED.display_name,
    permission_level = EXCLUDED.permission_level,
    is_active = EXCLUDED.is_active,
    updated_by_type = 'SYSTEM',
    is_deleted = FALSE,
    deleted_at = NULL,
    deleted_by_type = NULL,
    deleted_by_id = NULL;

-- 2) Products
INSERT INTO product_categories (
    name,
    display_order,
    is_published,
    created_by_type,
    updated_by_type
)
VALUES
    ('E2E-PUBLISHED', 10, TRUE, 'SYSTEM', 'SYSTEM'),
    ('E2E-HIDDEN', 20, FALSE, 'SYSTEM', 'SYSTEM')
ON CONFLICT (name) DO UPDATE
SET
    display_order = EXCLUDED.display_order,
    is_published = EXCLUDED.is_published,
    updated_by_type = 'SYSTEM',
    is_deleted = FALSE,
    deleted_at = NULL,
    deleted_by_type = NULL,
    deleted_by_id = NULL;

INSERT INTO products (
    product_code,
    name,
    description,
    price,
    stock,
    category_id,
    image,
    is_published,
    allocation_type,
    created_by_type,
    updated_by_type
)
SELECT
    'P-E2E-0001',
    'E2E Real In Stock',
    'Published product with REAL allocation',
    12000,
    30,
    c.id,
    'https://placehold.co/600x800/E7E5E4/71717A?text=Product+1',
    TRUE,
    'REAL',
    'SYSTEM',
    'SYSTEM'
FROM product_categories c
WHERE c.name = 'E2E-PUBLISHED'
ON CONFLICT (product_code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    stock = EXCLUDED.stock,
    category_id = EXCLUDED.category_id,
    image = EXCLUDED.image,
    is_published = EXCLUDED.is_published,
    allocation_type = EXCLUDED.allocation_type,
    updated_by_type = 'SYSTEM',
    is_deleted = FALSE,
    deleted_at = NULL,
    deleted_by_type = NULL,
    deleted_by_id = NULL;

INSERT INTO products (
    product_code,
    name,
    description,
    price,
    stock,
    category_id,
    image,
    is_published,
    allocation_type,
    created_by_type,
    updated_by_type
)
SELECT
    'P-E2E-0002',
    'E2E Hidden Product',
    'Unpublished product',
    8000,
    12,
    c.id,
    'https://placehold.co/600x800/E7E5E4/71717A?text=Product+1',
    FALSE,
    'REAL',
    'SYSTEM',
    'SYSTEM'
FROM product_categories c
WHERE c.name = 'E2E-PUBLISHED'
ON CONFLICT (product_code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    stock = EXCLUDED.stock,
    category_id = EXCLUDED.category_id,
    image = EXCLUDED.image,
    is_published = EXCLUDED.is_published,
    allocation_type = EXCLUDED.allocation_type,
    updated_by_type = 'SYSTEM',
    is_deleted = FALSE,
    deleted_at = NULL,
    deleted_by_type = NULL,
    deleted_by_id = NULL;

INSERT INTO products (
    product_code,
    name,
    description,
    price,
    stock,
    category_id,
    image,
    is_published,
    allocation_type,
    created_by_type,
    updated_by_type
)
SELECT
    'P-E2E-0003',
    'E2E Category Hidden Product',
    'Published product under hidden category',
    9000,
    15,
    c.id,
    'https://placehold.co/600x800/E7E5E4/71717A?text=Product+1',
    TRUE,
    'REAL',
    'SYSTEM',
    'SYSTEM'
FROM product_categories c
WHERE c.name = 'E2E-HIDDEN'
ON CONFLICT (product_code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    stock = EXCLUDED.stock,
    category_id = EXCLUDED.category_id,
    image = EXCLUDED.image,
    is_published = EXCLUDED.is_published,
    allocation_type = EXCLUDED.allocation_type,
    updated_by_type = 'SYSTEM',
    is_deleted = FALSE,
    deleted_at = NULL,
    deleted_by_type = NULL,
    deleted_by_id = NULL;

INSERT INTO products (
    product_code,
    name,
    description,
    price,
    stock,
    category_id,
    image,
    is_published,
    allocation_type,
    created_by_type,
    updated_by_type
)
SELECT
    'P-E2E-0004',
    'E2E Frame Product',
    'Published product with FRAME allocation',
    15000,
    0,
    c.id,
    'https://placehold.co/600x800/E7E5E4/71717A?text=Product+1',
    TRUE,
    'FRAME',
    'SYSTEM',
    'SYSTEM'
FROM product_categories c
WHERE c.name = 'E2E-PUBLISHED'
ON CONFLICT (product_code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    stock = EXCLUDED.stock,
    category_id = EXCLUDED.category_id,
    image = EXCLUDED.image,
    is_published = EXCLUDED.is_published,
    allocation_type = EXCLUDED.allocation_type,
    updated_by_type = 'SYSTEM',
    is_deleted = FALSE,
    deleted_at = NULL,
    deleted_by_type = NULL,
    deleted_by_id = NULL;

INSERT INTO products (
    product_code,
    name,
    description,
    price,
    stock,
    category_id,
    image,
    is_published,
    allocation_type,
    created_by_type,
    updated_by_type
)
SELECT
    'P-E2E-0005',
    'E2E Real Out Of Stock',
    'Published product with REAL allocation and zero stock',
    6000,
    0,
    c.id,
    'https://placehold.co/600x800/E7E5E4/71717A?text=Product+1',
    TRUE,
    'REAL',
    'SYSTEM',
    'SYSTEM'
FROM product_categories c
WHERE c.name = 'E2E-PUBLISHED'
ON CONFLICT (product_code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    stock = EXCLUDED.stock,
    category_id = EXCLUDED.category_id,
    image = EXCLUDED.image,
    is_published = EXCLUDED.is_published,
    allocation_type = EXCLUDED.allocation_type,
    updated_by_type = 'SYSTEM',
    is_deleted = FALSE,
    deleted_at = NULL,
    deleted_by_type = NULL,
    deleted_by_id = NULL;

-- 3) Inventory
INSERT INTO location_stocks (
    product_id,
    location_id,
    available_qty,
    committed_qty,
    created_by_type,
    updated_by_type
)
SELECT
    p.id,
    1,
    CASE p.product_code
        WHEN 'P-E2E-0001' THEN 30
        WHEN 'P-E2E-0005' THEN 0
        ELSE 5
    END,
    0,
    'SYSTEM',
    'SYSTEM'
FROM products p
WHERE p.product_code IN ('P-E2E-0001', 'P-E2E-0005')
ON CONFLICT (product_id, location_id) DO UPDATE
SET
    available_qty = EXCLUDED.available_qty,
    committed_qty = EXCLUDED.committed_qty,
    updated_by_type = 'SYSTEM',
    is_deleted = FALSE,
    deleted_at = NULL,
    deleted_by_type = NULL,
    deleted_by_id = NULL;

INSERT INTO sales_limits (
    product_id,
    frame_limit_qty,
    created_by_type,
    updated_by_type
)
SELECT
    p.id,
    40,
    'SYSTEM',
    'SYSTEM'
FROM products p
WHERE p.product_code = 'P-E2E-0004'
ON CONFLICT (product_id) DO UPDATE
SET
    frame_limit_qty = EXCLUDED.frame_limit_qty,
    updated_by_type = 'SYSTEM',
    is_deleted = FALSE,
    deleted_at = NULL,
    deleted_by_type = NULL,
    deleted_by_id = NULL;
