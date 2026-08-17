ALTER TABLE outbox_event
    ADD COLUMN next_attempt_at TIMESTAMPTZ,
    ADD COLUMN claim_token UUID,
    ADD COLUMN claimed_until TIMESTAMPTZ,
    ADD COLUMN last_error_code VARCHAR(100);

UPDATE outbox_event
SET next_attempt_at = created_at
WHERE next_attempt_at IS NULL;

ALTER TABLE outbox_event
    ALTER COLUMN next_attempt_at SET NOT NULL;

DROP INDEX ix_outbox_event_publish_queue;

CREATE INDEX ix_outbox_event_publish_queue
    ON outbox_event (status, next_attempt_at, created_at);

CREATE INDEX ix_outbox_event_expired_lease
    ON outbox_event (claimed_until)
    WHERE status = 'PROCESSING';

