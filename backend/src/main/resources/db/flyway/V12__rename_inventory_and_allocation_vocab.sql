ALTER TABLE location_stocks
    RENAME COLUMN allocatable_qty TO available_qty;

ALTER TABLE location_stocks
    RENAME COLUMN allocated_qty TO committed_qty;

ALTER TABLE order_items
    RENAME COLUMN allocated_qty TO committed_qty;

ALTER TABLE sales_limits
    RENAME COLUMN sales_limit_total TO frame_limit_qty;

ALTER TABLE location_stocks
    RENAME CONSTRAINT ck_location_stocks_allocatable_qty_non_negative TO ck_location_stocks_available_qty_non_negative;

ALTER TABLE location_stocks
    RENAME CONSTRAINT ck_location_stocks_allocated_qty_non_negative TO ck_location_stocks_committed_qty_non_negative;

ALTER TABLE order_items
    RENAME CONSTRAINT ck_order_items_allocated_qty_non_negative TO ck_order_items_committed_qty_non_negative;

ALTER TABLE order_items
    RENAME CONSTRAINT ck_order_items_allocated_not_exceed_quantity TO ck_order_items_committed_not_exceed_quantity;

ALTER TABLE sales_limits
    RENAME CONSTRAINT ck_sales_limits_total_non_negative TO ck_sales_limits_frame_limit_qty_non_negative;

ALTER INDEX idx_order_items_product_allocated_qty
    RENAME TO idx_order_items_product_committed_qty;
