-- E2E seed assertions (count + FK integrity)

DO $$
DECLARE
    actual_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO actual_count
      FROM users
     WHERE email IN ('member01@example.com', 'member02@example.com')
       AND is_deleted = FALSE;

    IF actual_count <> 2 THEN
        RAISE EXCEPTION 'E2E assertion failed: users count expected=2 actual=%', actual_count;
    END IF;
END
$$;

DO $$
DECLARE
    actual_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO actual_count
      FROM bo_users
     WHERE email = 'admin@example.com'
       AND is_deleted = FALSE;

    IF actual_count <> 1 THEN
        RAISE EXCEPTION 'E2E assertion failed: bo_users count expected=1 actual=%', actual_count;
    END IF;
END
$$;

DO $$
DECLARE
    actual_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO actual_count
      FROM product_categories
     WHERE name IN ('E2E-PUBLISHED', 'E2E-HIDDEN')
       AND is_deleted = FALSE;

    IF actual_count <> 2 THEN
        RAISE EXCEPTION 'E2E assertion failed: product_categories count expected=2 actual=%', actual_count;
    END IF;
END
$$;

DO $$
DECLARE
    actual_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO actual_count
      FROM products
     WHERE product_code IN ('P-E2E-0001', 'P-E2E-0002', 'P-E2E-0003', 'P-E2E-0004', 'P-E2E-0005')
       AND is_deleted = FALSE;

    IF actual_count <> 5 THEN
        RAISE EXCEPTION 'E2E assertion failed: products count expected=5 actual=%', actual_count;
    END IF;
END
$$;

DO $$
DECLARE
    actual_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO actual_count
      FROM location_stocks ls
      JOIN products p ON p.id = ls.product_id
     WHERE p.product_code IN ('P-E2E-0001', 'P-E2E-0005')
       AND ls.is_deleted = FALSE;

    IF actual_count <> 2 THEN
        RAISE EXCEPTION 'E2E assertion failed: location_stocks count expected=2 actual=%', actual_count;
    END IF;
END
$$;

DO $$
DECLARE
    actual_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO actual_count
      FROM sales_limits sl
      JOIN products p ON p.id = sl.product_id
     WHERE p.product_code = 'P-E2E-0004'
       AND sl.is_deleted = FALSE;

    IF actual_count <> 1 THEN
        RAISE EXCEPTION 'E2E assertion failed: sales_limits count expected=1 actual=%', actual_count;
    END IF;
END
$$;

DO $$
DECLARE
    actual_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO actual_count
      FROM products
     WHERE product_code IN ('P-E2E-0001', 'P-E2E-0002', 'P-E2E-0003', 'P-E2E-0004', 'P-E2E-0005')
       AND image = 'https://placehold.co/600x800/E7E5E4/71717A?text=Product+1'
       AND is_deleted = FALSE;

    IF actual_count <> 5 THEN
        RAISE EXCEPTION 'E2E assertion failed: products image alignment expected=5 actual=%', actual_count;
    END IF;
END
$$;

DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
      FROM products p
      LEFT JOIN product_categories c ON c.id = p.category_id
     WHERE p.product_code LIKE 'P-E2E-%'
       AND c.id IS NULL;

    IF orphan_count <> 0 THEN
        RAISE EXCEPTION 'E2E assertion failed: products.category_id orphan_count=%', orphan_count;
    END IF;
END
$$;

DO $$
DECLARE
    status_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO status_count
      FROM (
        SELECT o.status
          FROM orders o
          JOIN users u ON u.id = o.user_id
          JOIN order_items oi ON oi.order_id = o.id
          JOIN products p ON p.id = oi.product_id
         WHERE u.email = 'member01@example.com'
           AND p.product_code LIKE 'P-E2E-%'
           AND o.is_deleted = FALSE
         GROUP BY o.status
      ) statuses;

    IF status_count <> 6 THEN
        RAISE EXCEPTION 'E2E assertion failed: order statuses expected=6 actual=%', status_count;
    END IF;
END
$$;

DO $$
DECLARE
    allocated_count INTEGER;
    unallocated_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO allocated_count
      FROM (
        SELECT o.id
          FROM orders o
          JOIN users u ON u.id = o.user_id
          JOIN order_items oi ON oi.order_id = o.id
          JOIN products p ON p.id = oi.product_id
         WHERE u.email = 'member01@example.com'
           AND p.product_code LIKE 'P-E2E-%'
           AND o.is_deleted = FALSE
         GROUP BY o.id
        HAVING SUM(oi.committed_qty) = SUM(oi.quantity)
           AND SUM(oi.quantity) > 0
      ) allocated_orders;

    SELECT COUNT(*) INTO unallocated_count
      FROM (
        SELECT o.id
          FROM orders o
          JOIN users u ON u.id = o.user_id
          JOIN order_items oi ON oi.order_id = o.id
          JOIN products p ON p.id = oi.product_id
         WHERE u.email = 'member01@example.com'
           AND p.product_code LIKE 'P-E2E-%'
           AND o.is_deleted = FALSE
         GROUP BY o.id
        HAVING SUM(oi.committed_qty) < SUM(oi.quantity)
      ) unallocated_orders;

    IF allocated_count < 1 THEN
        RAISE EXCEPTION 'E2E assertion failed: allocated orders expected>=1 actual=%', allocated_count;
    END IF;

    IF unallocated_count < 1 THEN
        RAISE EXCEPTION 'E2E assertion failed: unallocated orders expected>=1 actual=%', unallocated_count;
    END IF;
END
$$;

DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
      FROM location_stocks ls
      LEFT JOIN products p ON p.id = ls.product_id
     WHERE p.id IS NULL;

    IF orphan_count <> 0 THEN
        RAISE EXCEPTION 'E2E assertion failed: location_stocks.product_id orphan_count=%', orphan_count;
    END IF;
END
$$;

DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
      FROM sales_limits sl
      LEFT JOIN products p ON p.id = sl.product_id
     WHERE p.id IS NULL;

    IF orphan_count <> 0 THEN
        RAISE EXCEPTION 'E2E assertion failed: sales_limits.product_id orphan_count=%', orphan_count;
    END IF;
END
$$;
