package com.joajy.spendingguard.dashboard.repository;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.dashboard.service.port.MonthlyDashboardQuery;
import com.joajy.spendingguard.dashboard.service.result.BudgetSummary;
import com.joajy.spendingguard.dashboard.service.result.CategorySpending;
import com.joajy.spendingguard.dashboard.service.result.RiskCount;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** 대시보드 전용 집계 SQL을 쓰기 엔티티와 분리해 실행한다. */
@Component
class MonthlyDashboardPersistenceAdapter implements MonthlyDashboardQuery {

    private static final String CATEGORY_SPENDING = """
            SELECT COALESCE(category_override.category, parse.category) AS category,
                   SUM(parse.amount)::BIGINT AS amount,
                   COUNT(*) AS transaction_count
              FROM raw_spend_event event
              JOIN fast_parse_result parse ON parse.raw_event_id = event.id
              LEFT JOIN spend_category_override category_override
                ON category_override.spend_event_id = event.id
             WHERE event.user_id = :userId
               AND COALESCE(event.occurred_at, event.received_at) >= :from
               AND COALESCE(event.occurred_at, event.received_at) < :until
               AND parse.status = 'PARSED'
               AND parse.transaction_type = 'PAYMENT'
             GROUP BY COALESCE(category_override.category, parse.category)
             ORDER BY amount DESC, category
            """;

    private static final String RISK_COUNTS = """
            SELECT COALESCE(category_override.risk_level, parse.risk_level) AS risk_level,
                   COUNT(*) AS risk_count
              FROM raw_spend_event event
              JOIN fast_parse_result parse ON parse.raw_event_id = event.id
              LEFT JOIN spend_category_override category_override
                ON category_override.spend_event_id = event.id
             WHERE event.user_id = :userId
               AND COALESCE(event.occurred_at, event.received_at) >= :from
               AND COALESCE(event.occurred_at, event.received_at) < :until
               AND parse.status = 'PARSED'
               AND parse.transaction_type = 'PAYMENT'
             GROUP BY COALESCE(category_override.risk_level, parse.risk_level)
             ORDER BY CASE COALESCE(category_override.risk_level, parse.risk_level)
                      WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END
            """;

    private final JdbcClient jdbcClient;

    MonthlyDashboardPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean userExists(UUID userId) {
        return jdbcClient.sql("SELECT EXISTS(SELECT 1 FROM user_account WHERE id = :userId)")
                .param("userId", userId).query(Boolean.class).single();
    }

    @Override
    public Optional<BudgetSummary> findBudget(UUID userId, YearMonth month) {
        return jdbcClient.sql("""
                SELECT limit_amount, spent_amount, limit_amount - spent_amount AS remaining_amount
                  FROM monthly_budget
                 WHERE user_id = :userId AND budget_month = :month
                """).param("userId", userId).param("month", month.toString())
                .query((row, index) -> new BudgetSummary(
                        row.getLong("limit_amount"),
                        row.getLong("spent_amount"),
                        row.getLong("remaining_amount")
                )).optional();
    }

    @Override
    public List<CategorySpending> findCategorySpending(UUID userId, Instant from, Instant until) {
        return jdbcClient.sql(CATEGORY_SPENDING)
                .param("userId", userId)
                .param("from", from.atOffset(ZoneOffset.UTC))
                .param("until", until.atOffset(ZoneOffset.UTC))
                .query((row, index) -> new CategorySpending(
                        row.getString("category"),
                        row.getLong("amount"),
                        row.getLong("transaction_count")
                )).list();
    }

    @Override
    public List<RiskCount> findRiskCounts(UUID userId, Instant from, Instant until) {
        return jdbcClient.sql(RISK_COUNTS)
                .param("userId", userId)
                .param("from", from.atOffset(ZoneOffset.UTC))
                .param("until", until.atOffset(ZoneOffset.UTC))
                .query((row, index) -> new RiskCount(
                        row.getString("risk_level"), row.getLong("risk_count")
                )).list();
    }
}
