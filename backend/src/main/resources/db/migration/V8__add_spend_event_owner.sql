ALTER TABLE raw_spend_event
    ADD COLUMN user_id UUID REFERENCES user_account(id);

CREATE INDEX ix_raw_spend_event_user_received
    ON raw_spend_event (user_id, received_at DESC);
