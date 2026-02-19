DO $$
DECLARE
    status_constraint_name text;
BEGIN
    SELECT conname
      INTO status_constraint_name
      FROM pg_constraint
     WHERE conrelid = 'orders'::regclass
       AND contype = 'c'
       AND pg_get_constraintdef(oid) LIKE '%status%';

    IF status_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE orders DROP CONSTRAINT %I', status_constraint_name);
    END IF;
END $$;

ALTER TABLE orders
    ADD CONSTRAINT ck_orders_status
    CHECK (status IN ('PENDING', 'CONFIRMED', 'PREPARING_SHIPMENT', 'SHIPPED', 'DELIVERED', 'CANCELLED'));

CREATE TABLE job_run_history (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(100) NOT NULL UNIQUE,
    job_type VARCHAR(100) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED')),
    processed_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(2000),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_job_run_history_job_type_started_at ON job_run_history(job_type, started_at DESC);
CREATE INDEX idx_job_run_history_status ON job_run_history(status);

CREATE TABLE shipments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    shipment_type VARCHAR(50) NOT NULL CHECK (shipment_type IN ('OUTBOUND', 'RETURN')),
    status VARCHAR(50) NOT NULL CHECK (status IN ('READY', 'EXPORTED', 'TRANSFERRED')),
    export_file_path VARCHAR(500),

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

    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT uk_shipments_order_type UNIQUE (order_id, shipment_type)
);

CREATE INDEX idx_shipments_status ON shipments(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_shipments_order_id ON shipments(order_id);
CREATE INDEX idx_shipments_export_file_path ON shipments(export_file_path) WHERE is_deleted = FALSE;
CREATE INDEX idx_shipments_is_deleted ON shipments(is_deleted);

CREATE TRIGGER update_shipments_updated_at
    BEFORE UPDATE ON shipments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE shipment_items (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_price NUMERIC(10, 2) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    subtotal NUMERIC(10, 2) NOT NULL CHECK (subtotal >= 0),

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

    CONSTRAINT fk_shipment_items_shipment FOREIGN KEY (shipment_id)
        REFERENCES shipments(id) ON DELETE CASCADE,
    CONSTRAINT fk_shipment_items_order_item FOREIGN KEY (order_item_id)
        REFERENCES order_items(id) ON DELETE RESTRICT
);

CREATE INDEX idx_shipment_items_shipment_id ON shipment_items(shipment_id);
CREATE INDEX idx_shipment_items_order_item_id ON shipment_items(order_item_id);
CREATE INDEX idx_shipment_items_is_deleted ON shipment_items(is_deleted);

CREATE TRIGGER update_shipment_items_updated_at
    BEFORE UPDATE ON shipment_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
