package com.joajy.spendingguard.budget.repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import com.joajy.spendingguard.analysis.service.port.outbound.ApplyBudgetConsumptionPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 결제별 예산 반영 원장과 월 누적 사용액을 같은 트랜잭션에서 갱신한다. */
@Component
class BudgetConsumptionPersistenceAdapter implements ApplyBudgetConsumptionPort {

    private static final String RECORD_CONSUMPTION = """
            INSERT INTO budget_consumption (id, budget_id, spend_event_id, amount, applied_at)
            SELECT :id, budget.id, :eventId, :amount, :appliedAt
              FROM monthly_budget budget
             WHERE budget.user_id = :userId
               AND budget.budget_month = :month
            ON CONFLICT (spend_event_id) DO NOTHING
            RETURNING budget_id
            """;

    private static final String INCREASE_SPENT_AMOUNT = """
            UPDATE monthly_budget
               SET spent_amount = spent_amount + :amount,
                   version = version + 1,
                   updated_at = :appliedAt
             WHERE id = :budgetId
            """;

    private final JdbcClient jdbcClient;

    BudgetConsumptionPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public boolean apply(UUID userId, UUID eventId, YearMonth month, long amount, Instant appliedAt) {
        Map<String, Object> parameters = Map.of(
                "id", UUID.randomUUID(),
                "userId", userId,
                "eventId", eventId,
                "month", month.toString(),
                "amount", amount,
                "appliedAt", appliedAt.atOffset(ZoneOffset.UTC)
        );
        UUID budgetId = jdbcClient.sql(RECORD_CONSUMPTION)
                .params(parameters)
                .query(UUID.class)
                .optional()
                .orElse(null);
        if (budgetId == null) {
            return false;
        }
        int updated = jdbcClient.sql(INCREASE_SPENT_AMOUNT)
                .param("budgetId", budgetId)
                .param("amount", amount)
                .param("appliedAt", appliedAt.atOffset(ZoneOffset.UTC))
                .update();
        if (updated != 1) {
            throw new IllegalStateException("예산 사용액을 갱신할 수 없습니다.");
        }
        return true;
    }
}
