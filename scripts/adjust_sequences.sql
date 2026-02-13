SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM users), false);
SELECT setval('auth_tokens_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM auth_tokens), false);
SELECT setval('bo_users_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM bo_users), false);
SELECT setval('bo_auth_tokens_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM bo_auth_tokens), false);
SELECT setval('products_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM products), false);
SELECT setval('stock_reservations_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM stock_reservations), false);
SELECT setval('carts_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM carts), false);
SELECT setval('cart_items_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM cart_items), false);
SELECT setval('orders_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM orders), false);
SELECT setval('order_items_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM order_items), false);
SELECT setval('operation_histories_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM operation_histories), false);
SELECT setval('inventory_adjustments_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM inventory_adjustments), false);
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
