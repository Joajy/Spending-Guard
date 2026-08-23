package com.joajy.spendingguard.budget.controller;

import java.time.Instant;
import java.time.YearMonth;
import com.joajy.spendingguard.budget.domain.MonthlyBudget;

public record MonthlyBudgetResponse(YearMonth month, long limitAmount, long spentAmount,
                                    long remainingAmount, long version, Instant updatedAt) {
    static MonthlyBudgetResponse from(MonthlyBudget budget) {
        return new MonthlyBudgetResponse(budget.month(), budget.limitAmount(), budget.spentAmount(),
                budget.remainingAmount(), budget.version(), budget.updatedAt());
    }
}
