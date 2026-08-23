ALTER TABLE fast_parse_result
    ADD COLUMN category VARCHAR(30),
    ADD COLUMN fixed_cost BOOLEAN,
    ADD COLUMN risk_level VARCHAR(20),
    ADD COLUMN risk_reason VARCHAR(100),
    ADD CONSTRAINT ck_fast_parse_result_risk CHECK (
        (status = 'NEEDS_REVIEW' AND category IS NULL AND fixed_cost IS NULL
            AND risk_level IS NULL AND risk_reason IS NULL)
        OR (status = 'PARSED' AND category IS NOT NULL AND fixed_cost IS NOT NULL
            AND risk_level IS NOT NULL AND risk_reason IS NOT NULL)
    );
