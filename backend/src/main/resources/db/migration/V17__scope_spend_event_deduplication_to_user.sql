-- Existing fingerprints remain unchanged so retries of pre-migration events stay idempotent.
-- NULL owners represent legacy events and must retain duplicate protection as one scope.
ALTER TABLE raw_spend_event
    ADD CONSTRAINT uk_raw_spend_event_user_deduplication_key
        UNIQUE NULLS NOT DISTINCT (user_id, deduplication_key);

ALTER TABLE raw_spend_event
    DROP CONSTRAINT uk_raw_spend_event_deduplication_key;

DROP INDEX uk_raw_spend_event_source_external_event_id;

CREATE UNIQUE INDEX uk_raw_spend_event_user_source_external_event_id
    ON raw_spend_event (user_id, source, external_event_id) NULLS NOT DISTINCT
    WHERE external_event_id IS NOT NULL;
