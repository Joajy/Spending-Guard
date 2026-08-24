CREATE INDEX ix_raw_spend_event_user_transaction_cursor
    ON raw_spend_event (
        user_id,
        (COALESCE(occurred_at, received_at)) DESC,
        id DESC
    );
