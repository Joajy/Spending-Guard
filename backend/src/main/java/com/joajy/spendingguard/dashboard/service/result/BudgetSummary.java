package com.joajy.spendingguard.dashboard.service.result;

public record BudgetSummary(long limitAmount, long spentAmount, long remainingAmount) {
}
