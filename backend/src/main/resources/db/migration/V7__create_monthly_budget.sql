CREATE TABLE monthly_budget (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_account(id),
    budget_month VARCHAR(7) NOT NULL,
    limit_amount BIGINT NOT NULL CHECK (limit_amount > 0),
    spent_amount BIGINT NOT NULL DEFAULT 0 CHECK (spent_amount >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_monthly_budget_user_month UNIQUE (user_id, budget_month)
);
