ALTER TABLE budget_consumption
    DROP CONSTRAINT budget_consumption_amount_check;

ALTER TABLE budget_consumption
    ADD CONSTRAINT ck_budget_consumption_amount_nonzero CHECK (amount <> 0);
