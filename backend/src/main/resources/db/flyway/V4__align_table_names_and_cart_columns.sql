-- 既存環境向けの整合調整:
-- - reservations -> stock_reservations
-- - operation_history -> operation_histories
-- - carts / cart_items の追加カラムと制約を補完

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'reservations'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'stock_reservations'
    ) THEN
        ALTER TABLE reservations RENAME TO stock_reservations;
    END IF;
END;
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'operation_history'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'operation_histories'
    ) THEN
        ALTER TABLE operation_history RENAME TO operation_histories;
    END IF;
END;
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_class
        WHERE relkind = 'S' AND relname = 'reservations_id_seq'
    ) AND NOT EXISTS (
        SELECT 1
        FROM pg_class
        WHERE relkind = 'S' AND relname = 'stock_reservations_id_seq'
    ) THEN
        ALTER SEQUENCE reservations_id_seq RENAME TO stock_reservations_id_seq;
    END IF;
END;
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_class
        WHERE relkind = 'S' AND relname = 'operation_history_id_seq'
    ) AND NOT EXISTS (
        SELECT 1
        FROM pg_class
        WHERE relkind = 'S' AND relname = 'operation_histories_id_seq'
    ) THEN
        ALTER SEQUENCE operation_history_id_seq RENAME TO operation_histories_id_seq;
    END IF;
END;
$$;

ALTER TABLE carts ADD COLUMN IF NOT EXISTS user_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_carts_user'
          AND conrelid = 'carts'::regclass
    ) THEN
        ALTER TABLE carts
            ADD CONSTRAINT fk_carts_user
            FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE SET NULL;
    END IF;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_carts_user_id ON carts(user_id);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'carts'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE carts
            ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE
            USING created_at AT TIME ZONE 'UTC';
    END IF;
END;
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'carts'
          AND column_name = 'updated_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE carts
            ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE
            USING updated_at AT TIME ZONE 'UTC';
    END IF;
END;
$$;

ALTER TABLE cart_items
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE cart_items
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'cart_items'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE cart_items
            ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE
            USING created_at AT TIME ZONE 'UTC';
    END IF;
END;
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'cart_items'
          AND column_name = 'updated_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE cart_items
            ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE
            USING updated_at AT TIME ZONE 'UTC';
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_cart_items_cart_product'
          AND conrelid = 'cart_items'::regclass
    ) THEN
        ALTER TABLE cart_items
            ADD CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'update_cart_items_updated_at'
          AND tgrelid = 'cart_items'::regclass
    ) THEN
        CREATE TRIGGER update_cart_items_updated_at
            BEFORE UPDATE ON cart_items
            FOR EACH ROW
            EXECUTE FUNCTION update_updated_at_column();
    END IF;
END;
$$;
