CREATE TABLE budget_consumption (
    id UUID PRIMARY KEY,
    budget_id UUID NOT NULL REFERENCES monthly_budget(id),
    spend_event_id UUID NOT NULL REFERENCES raw_spend_event(id),
    amount BIGINT NOT NULL CHECK (amount > 0),
    applied_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_budget_consumption_spend_event UNIQUE (spend_event_id)
);

CREATE INDEX ix_budget_consumption_budget_applied
    ON budget_consumption (budget_id, applied_at DESC);
