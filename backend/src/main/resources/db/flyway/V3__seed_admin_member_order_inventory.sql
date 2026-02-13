-- ============================================
-- 管理画面・業務確認用の初期データ投入
-- ============================================
-- 管理者ログイン:
--   email    : admin@example.com
--   password : password
--
-- 注:
-- - 既存データがある環境でも重複しないように NOT EXISTS で投入する
-- - パスワードハッシュは BCrypt ("password")

-- 1) 管理画面用 BoUser（ADMIN）
INSERT INTO bo_users (
    email,
    password_hash,
    display_name,
    permission_level,
    is_active,
    created_by_type
)
SELECT
    'admin@example.com',
    '$2a$10$.er/VYSjKnsPJ4Lx4XLg2u5LS/htFxp2QdD/lC.BpozDocM1R2bgS',
    'admin',
    'ADMIN',
    TRUE,
    'SYSTEM'
WHERE NOT EXISTS (
    SELECT 1 FROM bo_users WHERE email = 'admin@example.com'
);

-- 2) 管理画面会員一覧用の会員データ
INSERT INTO users (
    email,
    password_hash,
    display_name,
    is_active,
    created_by_type
)
SELECT
    'member01@example.com',
    '$2a$10$.er/VYSjKnsPJ4Lx4XLg2u5LS/htFxp2QdD/lC.BpozDocM1R2bgS',
    'サンプル会員01',
    TRUE,
    'SYSTEM'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'member01@example.com'
);

-- 3) 注文データ（会員注文）を1件作成
WITH target_user AS (
    SELECT id
    FROM users
    WHERE email = 'member01@example.com'
),
target_product AS (
    SELECT id, price
    FROM products
    WHERE is_deleted = FALSE
    ORDER BY id
    LIMIT 1
)
INSERT INTO orders (
    order_number,
    user_id,
    session_id,
    total_price,
    status,
    created_by_type
)
SELECT
    'ORD-SEED-0001',
    u.id,
    'seed-session-001',
    p.price,
    'PENDING',
    'SYSTEM'
FROM target_user u
CROSS JOIN target_product p
WHERE NOT EXISTS (
    SELECT 1 FROM orders WHERE order_number = 'ORD-SEED-0001'
);

-- 4) 注文明細を1件作成
WITH target_order AS (
    SELECT id
    FROM orders
    WHERE order_number = 'ORD-SEED-0001'
),
target_product AS (
    SELECT id, name, price
    FROM products
    WHERE is_deleted = FALSE
    ORDER BY id
    LIMIT 1
)
INSERT INTO order_items (
    order_id,
    product_id,
    product_name,
    product_price,
    quantity,
    subtotal,
    created_by_type
)
SELECT
    o.id,
    p.id,
    p.name,
    p.price,
    1,
    p.price,
    'SYSTEM'
FROM target_order o
CROSS JOIN target_product p
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items oi
    WHERE oi.order_id = o.id
      AND oi.product_id = p.id
      AND oi.is_deleted = FALSE
);

-- 5) 在庫画面確認用の仮引当を1件作成（30分有効）
WITH target_user AS (
    SELECT id
    FROM users
    WHERE email = 'member01@example.com'
),
target_product AS (
    SELECT id
    FROM products
    WHERE is_deleted = FALSE
    ORDER BY id
    OFFSET 1
    LIMIT 1
)
INSERT INTO reservations (
    product_id,
    quantity,
    session_id,
    user_id,
    reservation_type,
    expires_at,
    created_by_type
)
SELECT
    p.id,
    2,
    'seed-session-001',
    u.id,
    'TENTATIVE',
    CURRENT_TIMESTAMP + INTERVAL '30 minutes',
    'SYSTEM'
FROM target_user u
CROSS JOIN target_product p
WHERE NOT EXISTS (
    SELECT 1
    FROM reservations r
    WHERE r.session_id = 'seed-session-001'
      AND r.product_id = p.id
      AND r.reservation_type = 'TENTATIVE'
      AND r.is_deleted = FALSE
);
