-- 既存の非標準注文番号を ORD-xxxxxxxxxx 形式へ正規化
WITH current_max AS (
    SELECT COALESCE(MAX(CAST(SUBSTRING(order_number FROM 5) AS BIGINT)), 0) AS max_seq
    FROM orders
    WHERE order_number ~ '^ORD-[0-9]{10}$'
),
legacy_orders AS (
    SELECT
        id,
        ROW_NUMBER() OVER (ORDER BY created_at, id) AS seq_offset
    FROM orders
    WHERE order_number !~ '^ORD-[0-9]{10}$'
),
renumber_map AS (
    SELECT
        l.id,
        c.max_seq + l.seq_offset AS next_seq
    FROM legacy_orders l
    CROSS JOIN current_max c
)
UPDATE orders o
SET order_number = 'ORD-' || LPAD(renumber_map.next_seq::text, 10, '0')
FROM renumber_map
WHERE o.id = renumber_map.id;

-- 注文番号採番シーケンスを最新値に同期
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
