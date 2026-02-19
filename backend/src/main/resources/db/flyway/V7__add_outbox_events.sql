CREATE TYPE outbox_event_status AS ENUM ('PENDING', 'PROCESSING', 'PROCESSED', 'DEAD');

CREATE TABLE outbox_events (
    id            BIGSERIAL PRIMARY KEY,
    event_type    VARCHAR(100)          NOT NULL,
    aggregate_id  VARCHAR(255),
    payload       JSONB                 NOT NULL,
    status        outbox_event_status   NOT NULL DEFAULT 'PENDING',
    retry_count   INT                   NOT NULL DEFAULT 0,
    max_retries   INT                   NOT NULL DEFAULT 3,
    error_message TEXT,
    scheduled_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at  TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_events_status_scheduled
    ON outbox_events (status, scheduled_at)
    WHERE status = 'PENDING';
