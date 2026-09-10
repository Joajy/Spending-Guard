CREATE TABLE toss_test_order (
    order_id VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_account(id),
    amount BIGINT NOT NULL CHECK (amount BETWEEN 100 AND 1000000),
    order_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMING', 'CONFIRMED', 'CANCELING', 'CANCELED')),
    payment_key VARCHAR(200) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX ix_toss_test_order_user_created
    ON toss_test_order (user_id, created_at DESC);
