package com.joajy.spendingguard.budget.domain;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

public record MonthlyBudget(UUID id, UUID userId, YearMonth month, long limitAmount,
                            long spentAmount, long version, Instant updatedAt) {
    public long remainingAmount() {
        return limitAmount - spentAmount;
    }
}
