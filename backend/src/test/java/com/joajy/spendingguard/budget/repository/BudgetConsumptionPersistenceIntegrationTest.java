package com.joajy.spendingguard.budget.repository;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

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
class BudgetConsumptionPersistenceIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("72672aba-d9d5-4a25-99b5-2d6b8bb4b612");
    private static final UUID BUDGET_ID = UUID.fromString("54f78b0b-4692-45b8-b334-f914ec24efdc");
    private static final Instant NOW = Instant.parse("2026-08-23T01:00:00Z");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private BudgetConsumptionPersistenceAdapter adapter;
    @Autowired private JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM budget_consumption").update();
        jdbcClient.sql("DELETE FROM monthly_budget").update();
        jdbcClient.sql("DELETE FROM raw_spend_event").update();
        jdbcClient.sql("DELETE FROM user_account").update();
        jdbcClient.sql("""
                INSERT INTO user_account (id, email, password_hash, created_at)
                VALUES (:id, 'budget@example.com', 'hash', :now)
                """).param("id", USER_ID).param("now", NOW.atOffset(ZoneOffset.UTC)).update();
        jdbcClient.sql("""
                INSERT INTO monthly_budget
                    (id, user_id, budget_month, limit_amount, spent_amount, version, updated_at)
                VALUES (:id, :userId, '2026-08', 500000, 0, 0, :now)
                """).param("id", BUDGET_ID).param("userId", USER_ID)
                .param("now", NOW.atOffset(ZoneOffset.UTC)).update();
    }

    @Test
    void appliesSameSpendEventOnlyOnce() {
        UUID eventId = insertSpendEvent("dedupe-budget-1");

        boolean first = adapter.apply(USER_ID, eventId, YearMonth.of(2026, 8), 12_800, NOW);
        boolean duplicate = adapter.apply(USER_ID, eventId, YearMonth.of(2026, 8), 12_800, NOW);

        assertThat(first).isTrue();
        assertThat(duplicate).isFalse();
        assertThat(spentAmount()).isEqualTo(12_800);
        assertThat(consumptionCount()).isOne();
    }

    @Test
    void appliesCancellationAsASeparateNegativeDelta() {
        UUID paymentId = insertSpendEvent("payment-budget-1");
        UUID cancellationId = insertSpendEvent("cancel-budget-1");

        assertThat(adapter.apply(USER_ID, paymentId, YearMonth.of(2026, 8), 12_800, NOW)).isTrue();
        assertThat(adapter.apply(USER_ID, cancellationId, YearMonth.of(2026, 8), -3_000, NOW)).isTrue();

        assertThat(spentAmount()).isEqualTo(9_800);
        assertThat(consumptionCount()).isEqualTo(2);
    }

    @Test
    void skipsConsumptionWhenMonthlyBudgetDoesNotExist() {
        UUID eventId = insertSpendEvent("no-budget-1");

        boolean applied = adapter.apply(USER_ID, eventId, YearMonth.of(2026, 9), 12_800, NOW);

        assertThat(applied).isFalse();
        assertThat(spentAmount()).isZero();
        assertThat(consumptionCount()).isZero();
    }

    @Test
    void accumulatesConcurrentPaymentsWithoutLostUpdates() throws Exception {
        int paymentCount = 8;
        var eventIds = java.util.stream.IntStream.range(0, paymentCount)
                .mapToObj(index -> insertSpendEvent("concurrent-budget-" + index))
                .toList();
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(paymentCount);

        try {
            var futures = eventIds.stream()
                    .map(eventId -> executor.submit(() -> {
                        start.await();
                        return adapter.apply(
                                USER_ID, eventId, YearMonth.of(2026, 8), 12_800, NOW
                        );
                    }))
                    .toList();
            start.countDown();

            for (var future : futures) {
                assertThat(future.get()).isTrue();
            }
            assertThat(spentAmount()).isEqualTo(102_400);
            assertThat(consumptionCount()).isEqualTo(paymentCount);
        } finally {
            executor.shutdownNow();
        }
    }

    private UUID insertSpendEvent(String deduplicationKey) {
        UUID eventId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO raw_spend_event
                    (id, user_id, source, deduplication_key, sanitized_message, status,
                     occurred_at, received_at)
                VALUES (:id, :userId, 'SIMULATOR', :deduplicationKey, '쿠팡 12,800원 결제',
                        'RECEIVED', :now, :now)
                """)
                .param("id", eventId)
                .param("userId", USER_ID)
                .param("deduplicationKey", deduplicationKey)
                .param("now", NOW.atOffset(ZoneOffset.UTC))
                .update();
        return eventId;
    }

    private long spentAmount() {
        return jdbcClient.sql("SELECT spent_amount FROM monthly_budget WHERE id = :id")
                .param("id", BUDGET_ID)
                .query(Long.class)
                .single();
    }

    private long consumptionCount() {
        return jdbcClient.sql("SELECT COUNT(*) FROM budget_consumption")
                .query(Long.class)
                .single();
    }
}
