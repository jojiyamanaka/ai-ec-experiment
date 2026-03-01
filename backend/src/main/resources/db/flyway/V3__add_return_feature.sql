ALTER TABLE orders
    ADD COLUMN delivered_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE shipments
    ADD COLUMN reason VARCHAR(500);

ALTER TABLE shipments
    ADD COLUMN rejection_reason VARCHAR(500);

ALTER TABLE shipments
    DROP CONSTRAINT IF EXISTS shipments_status_check;

ALTER TABLE shipments
    ADD CONSTRAINT ck_shipments_status
    CHECK (
        status IN (
            'READY',
            'EXPORTED',
            'TRANSFERRED',
            'RETURN_PENDING',
            'RETURN_APPROVED',
            'RETURN_CONFIRMED',
            'RETURN_CANCELLED'
        )
    );

ALTER TABLE products
    ADD COLUMN is_returnable BOOLEAN NOT NULL DEFAULT TRUE;
