package com.joajy.spendingguard.budget.service.port;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import com.joajy.spendingguard.budget.domain.MonthlyBudget;

public interface MonthlyBudgetStore {
    boolean accountExists(UUID userId);
    Optional<MonthlyBudget> find(UUID userId, YearMonth month);
    MonthlyBudget create(UUID userId, YearMonth month, long amount, Instant now);
    MonthlyBudget update(MonthlyBudget current, long amount, Instant now);
}
