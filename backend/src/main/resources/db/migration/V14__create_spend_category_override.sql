CREATE TABLE spend_category_override (
    spend_event_id UUID PRIMARY KEY REFERENCES fast_parse_result(raw_event_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_account(id),
    category VARCHAR(30) NOT NULL,
    fixed_cost BOOLEAN NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    risk_reason VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    corrected_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_spend_category_override_user_corrected
    ON spend_category_override (user_id, corrected_at DESC);
