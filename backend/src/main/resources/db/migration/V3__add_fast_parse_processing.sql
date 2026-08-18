CREATE TABLE processed_event (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES raw_spend_event (id),
    consumer_name VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_processed_event_consumer UNIQUE (event_id, consumer_name)
);

CREATE INDEX ix_processed_event_consumer_processed_at
    ON processed_event (consumer_name, processed_at);

CREATE TABLE fast_parse_result (
    raw_event_id UUID PRIMARY KEY REFERENCES raw_spend_event (id),
    amount NUMERIC(19, 2),
    transaction_type VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    review_reason VARCHAR(200),
    parser_version VARCHAR(50) NOT NULL,
    parsed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_fast_parse_result_amount CHECK (amount IS NULL OR amount > 0),
    CONSTRAINT ck_fast_parse_result_review CHECK (
        (status = 'PARSED' AND amount IS NOT NULL
            AND transaction_type IS NOT NULL AND review_reason IS NULL)
        OR (status = 'NEEDS_REVIEW' AND review_reason IS NOT NULL)
    )
);
