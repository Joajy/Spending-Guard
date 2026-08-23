package com.joajy.spendingguard.dashboard.service.result;

import java.time.YearMonth;
import java.util.List;

public record MonthlyDashboard(
        YearMonth month,
        BudgetSummary budget,
        long totalSpending,
        long transactionCount,
        List<CategorySpending> categories,
        List<RiskCount> risks
) {
}
