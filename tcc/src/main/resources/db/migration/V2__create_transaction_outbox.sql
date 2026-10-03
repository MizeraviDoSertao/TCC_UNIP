CREATE TABLE IF NOT EXISTS ops.transaction_outbox (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    CONSTRAINT uk_transaction_outbox_aggregate UNIQUE (aggregate_id, event_type)
);

CREATE INDEX IF NOT EXISTS idx_transaction_outbox_pending
    ON ops.transaction_outbox (status, created_at);
