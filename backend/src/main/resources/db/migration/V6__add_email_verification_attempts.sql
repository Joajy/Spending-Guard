ALTER TABLE email_verification_challenge
    ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE email_verification_challenge
    ADD CONSTRAINT ck_email_verification_failed_attempts CHECK (failed_attempts >= 0);
