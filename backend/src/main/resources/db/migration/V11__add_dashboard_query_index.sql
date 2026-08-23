CREATE INDEX ix_raw_spend_event_user_occurred_at
    ON raw_spend_event (user_id, occurred_at DESC)
    WHERE user_id IS NOT NULL;
