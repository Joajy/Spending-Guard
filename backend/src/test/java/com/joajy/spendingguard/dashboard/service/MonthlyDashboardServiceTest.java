package com.joajy.spendingguard.dashboard.service;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.account.service.exception.UserAccountNotFoundException;
import com.joajy.spendingguard.dashboard.service.port.MonthlyDashboardQuery;
import com.joajy.spendingguard.dashboard.service.result.BudgetSummary;
import com.joajy.spendingguard.dashboard.service.result.CategorySpending;
import com.joajy.spendingguard.dashboard.service.result.RiskCount;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class MonthlyDashboardServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private final MonthlyDashboardQuery query = mock(MonthlyDashboardQuery.class);
    private final MonthlyDashboardService service = new MonthlyDashboardService(query);

    @Test
    void combinesBudgetCategoryAndRiskSummary() {
        given(query.userExists(USER_ID)).willReturn(true);
        given(query.findBudget(USER_ID, YearMonth.of(2026, 8)))
                .willReturn(Optional.of(new BudgetSummary(500_000, 40_800, 459_200)));
        given(query.findCategorySpending(any(), any(Instant.class), any(Instant.class)))
                .willReturn(List.of(
                        new CategorySpending("TRANSPORT", 28_000, 1),
                        new CategorySpending("SHOPPING", 12_800, 1)
                ));
        given(query.findRiskCounts(any(), any(Instant.class), any(Instant.class)))
                .willReturn(List.of(new RiskCount("HIGH", 1), new RiskCount("LOW", 1)));

        var result = service.get(USER_ID, YearMonth.of(2026, 8));

        assertThat(result.totalSpending()).isEqualTo(40_800);
        assertThat(result.transactionCount()).isEqualTo(2);
        assertThat(result.budget().remainingAmount()).isEqualTo(459_200);
        assertThat(result.categories()).hasSize(2);
        assertThat(result.risks()).hasSize(2);
    }

    @Test
    void returnsSummaryWithoutBudgetWhenUserHasNotConfiguredOne() {
        given(query.userExists(USER_ID)).willReturn(true);
        given(query.findBudget(USER_ID, YearMonth.of(2026, 8))).willReturn(Optional.empty());
        given(query.findCategorySpending(any(), any(), any())).willReturn(List.of());
        given(query.findRiskCounts(any(), any(), any())).willReturn(List.of());

        var result = service.get(USER_ID, YearMonth.of(2026, 8));

        assertThat(result.budget()).isNull();
        assertThat(result.totalSpending()).isZero();
    }

    @Test
    void rejectsUnknownUser() {
        given(query.userExists(USER_ID)).willReturn(false);

        assertThatThrownBy(() -> service.get(USER_ID, YearMonth.of(2026, 8)))
                .isInstanceOf(UserAccountNotFoundException.class);
    }
}
