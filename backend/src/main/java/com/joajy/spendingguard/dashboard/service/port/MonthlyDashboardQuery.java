package com.joajy.spendingguard.dashboard.service.port;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.dashboard.service.result.BudgetSummary;
import com.joajy.spendingguard.dashboard.service.result.CategorySpending;
import com.joajy.spendingguard.dashboard.service.result.RiskCount;

public interface MonthlyDashboardQuery {
    boolean userExists(UUID userId);
    Optional<BudgetSummary> findBudget(UUID userId, YearMonth month);
    List<CategorySpending> findCategorySpending(UUID userId, Instant from, Instant until);
    List<RiskCount> findRiskCounts(UUID userId, Instant from, Instant until);
}
