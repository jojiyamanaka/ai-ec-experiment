-- 注文番号採番用シーケンス
CREATE SEQUENCE IF NOT EXISTS order_number_seq START 1;

-- 旧シード値を新フォーマットへ統一
UPDATE orders
SET order_number = 'ORD-0000000001'
WHERE order_number = 'ORD-SEED-0001'
  AND NOT EXISTS (
    SELECT 1 FROM orders WHERE order_number = 'ORD-0000000001'
  );

-- 既存の10桁注文番号に追従してシーケンスを調整
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
