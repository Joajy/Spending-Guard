CREATE TABLE raw_spend_event (
    id UUID PRIMARY KEY,
    source VARCHAR(30) NOT NULL,
    external_event_id VARCHAR(200),
    deduplication_key VARCHAR(64) NOT NULL,
    sanitized_message VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    occurred_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_raw_spend_event_deduplication_key UNIQUE (deduplication_key)
);

CREATE UNIQUE INDEX uk_raw_spend_event_source_external_event_id
    ON raw_spend_event (source, external_event_id)
    WHERE external_event_id IS NOT NULL;

CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    CONSTRAINT uk_outbox_event_aggregate_type UNIQUE (aggregate_id, event_type),
    CONSTRAINT ck_outbox_event_attempt_count CHECK (attempt_count >= 0)
);

CREATE INDEX ix_outbox_event_publish_queue
    ON outbox_event (status, created_at);
