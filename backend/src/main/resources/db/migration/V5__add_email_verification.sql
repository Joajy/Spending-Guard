ALTER TABLE user_account ADD COLUMN email_verified_at TIMESTAMPTZ;

CREATE TABLE email_verification_challenge (
    user_id UUID PRIMARY KEY REFERENCES user_account(id) ON DELETE CASCADE,
    code_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
