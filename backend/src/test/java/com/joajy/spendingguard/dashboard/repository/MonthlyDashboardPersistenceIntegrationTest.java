package com.joajy.spendingguard.dashboard.repository;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spending-guard.outbox.publisher.enabled=false",
        "spending-guard.analysis.consumer.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class MonthlyDashboardPersistenceIntegrationTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID BUDGET_ID = UUID.randomUUID();
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MonthlyDashboardPersistenceAdapter adapter;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        var now = Instant.parse("2026-08-23T01:00:00Z").atOffset(ZoneOffset.UTC);
        jdbcClient.sql("DELETE FROM budget_consumption").update();
        jdbcClient.sql("DELETE FROM monthly_budget").update();
        jdbcClient.sql("DELETE FROM fast_parse_result").update();
        jdbcClient.sql("DELETE FROM outbox_event").update();
        jdbcClient.sql("DELETE FROM raw_spend_event").update();
        jdbcClient.sql("DELETE FROM user_account").update();
        jdbcClient.sql("""
                INSERT INTO user_account (id, email, password_hash, created_at)
                VALUES (:id, 'dashboard@example.com', 'hash', :now)
                """).param("id", USER_ID).param("now", now).update();
        jdbcClient.sql("""
                INSERT INTO monthly_budget
                    (id, user_id, budget_month, limit_amount, spent_amount, version, updated_at)
                VALUES (:id, :userId, '2026-08', 500000, 40800, 2, :now)
                """).param("id", BUDGET_ID).param("userId", USER_ID).param("now", now).update();
        insertParsedPayment("2026-08-11T10:00:00+09:00", 12_800, "SHOPPING", "LOW", "event-1");
        insertParsedPayment("2026-08-12T02:00:00+09:00", 28_000, "TRANSPORT", "HIGH", "event-2");
        insertParsedPayment("2026-09-01T00:00:00+09:00", 90_000, "DELIVERY", "MEDIUM", "event-3");
    }

    @Test
    void aggregatesOnlyPaymentsInsideRequestedMonth() {
        Instant from = YearMonth.of(2026, 8).atDay(1).atStartOfDay(SEOUL).toInstant();
        Instant until = YearMonth.of(2026, 9).atDay(1).atStartOfDay(SEOUL).toInstant();

        var budget = adapter.findBudget(USER_ID, YearMonth.of(2026, 8)).orElseThrow();
        var categories = adapter.findCategorySpending(USER_ID, from, until);
        var risks = adapter.findRiskCounts(USER_ID, from, until);

        assertThat(adapter.userExists(USER_ID)).isTrue();
        assertThat(budget.remainingAmount()).isEqualTo(459_200);
        assertThat(categories).extracting("category").containsExactly("TRANSPORT", "SHOPPING");
        assertThat(categories).extracting("amount").containsExactly(28_000L, 12_800L);
        assertThat(risks).extracting("riskLevel").containsExactly("HIGH", "LOW");
        assertThat(risks).extracting("count").containsExactly(1L, 1L);
    }

    private void insertParsedPayment(
            String occurredAt,
            long amount,
            String category,
            String riskLevel,
            String suffix
    ) {
        UUID eventId = UUID.randomUUID();
        var timestamp = java.time.OffsetDateTime.parse(occurredAt);
        jdbcClient.sql("""
                INSERT INTO raw_spend_event
                    (id, user_id, source, deduplication_key, sanitized_message, status,
                     occurred_at, received_at)
                VALUES (:id, :userId, 'SIMULATOR', :dedupe, '테스트 결제', 'ANALYZING',
                        :occurredAt, :occurredAt)
                """).param("id", eventId).param("userId", USER_ID)
                .param("dedupe", "dashboard-" + suffix).param("occurredAt", timestamp).update();
        jdbcClient.sql("""
                INSERT INTO fast_parse_result
                    (raw_event_id, amount, transaction_type, status, category, fixed_cost,
                     risk_level, risk_reason, parser_version, parsed_at)
                VALUES (:eventId, :amount, 'PAYMENT', 'PARSED', :category, false,
                        :riskLevel, 'TEST_REASON', 'fast-parser-v1', :parsedAt)
                """).param("eventId", eventId).param("amount", amount)
                .param("category", category).param("riskLevel", riskLevel)
                .param("parsedAt", timestamp).update();
    }
}
