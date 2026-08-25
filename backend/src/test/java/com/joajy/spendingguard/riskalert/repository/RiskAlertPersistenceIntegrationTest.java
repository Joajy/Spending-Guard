package com.joajy.spendingguard.riskalert.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.riskalert.service.model.RiskAlertQuery;
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
class RiskAlertPersistenceIntegrationTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RiskAlertPersistenceAdapter adapter;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        var now = Instant.parse("2026-08-20T00:00:00Z").atOffset(ZoneOffset.UTC);
        jdbcClient.sql("DELETE FROM spend_category_override").update();
        jdbcClient.sql("DELETE FROM fast_parse_result").update();
        jdbcClient.sql("DELETE FROM outbox_event").update();
        jdbcClient.sql("DELETE FROM raw_spend_event").update();
        jdbcClient.sql("DELETE FROM user_account").update();
        jdbcClient.sql("""
                INSERT INTO user_account (id, email, password_hash, created_at)
                VALUES (:id, 'risk-alert@example.com', 'hash', :now)
                """).param("id", USER_ID).param("now", now).update();
    }

    @Test
    void returnsOnlyEffectiveRiskLevelsInsideRequestedMonth() {
        insertParsedPayment("2026-08-20T10:00:00+09:00", "HIGH", "LARGE_PAYMENT");
        UUID corrected = insertParsedPayment(
                "2026-08-19T10:00:00+09:00", "HIGH", "LARGE_PAYMENT"
        );
        insertOverride(corrected, "MEDIUM", "ELEVATED_AMOUNT");
        insertParsedPayment("2026-08-18T10:00:00+09:00", "LOW", "NORMAL_PATTERN");
        insertParsedPayment("2026-09-01T00:00:00+09:00", "HIGH", "LARGE_PAYMENT");

        var mediumAlerts = adapter.find(query(RiskLevel.MEDIUM, null, null, 10));
        var highAlerts = adapter.find(query(RiskLevel.HIGH, null, null, 10));

        assertThat(adapter.userExists(USER_ID)).isTrue();
        assertThat(mediumAlerts).extracting("riskLevel")
                .containsExactly(RiskLevel.HIGH, RiskLevel.MEDIUM);
        assertThat(mediumAlerts.get(1).categoryVersion()).isEqualTo(1);
        assertThat(mediumAlerts.get(1).reasonCode()).isEqualTo("ELEVATED_AMOUNT");
        assertThat(highAlerts).extracting("riskLevel").containsExactly(RiskLevel.HIGH);
    }

    @Test
    void appliesCompositeCursorWithoutDuplicates() {
        insertParsedPayment("2026-08-20T10:00:00+09:00", "HIGH", "LARGE_PAYMENT");
        insertParsedPayment("2026-08-19T10:00:00+09:00", "MEDIUM", "ELEVATED_AMOUNT");
        insertParsedPayment("2026-08-18T10:00:00+09:00", "MEDIUM", "ELEVATED_AMOUNT");

        var firstPage = adapter.find(query(RiskLevel.MEDIUM, null, null, 2));
        var last = firstPage.get(1);
        var secondPage = adapter.find(query(
                RiskLevel.MEDIUM, last.transactionAt(), last.eventId(), 2
        ));

        assertThat(firstPage).hasSize(2);
        assertThat(secondPage).hasSize(1);
        assertThat(secondPage.get(0).eventId()).isNotIn(
                firstPage.stream().map(item -> item.eventId()).toList()
        );
    }

    private RiskAlertQuery query(
            RiskLevel minimumLevel,
            Instant cursorAt,
            UUID cursorId,
            int limit
    ) {
        Instant from = YearMonth.of(2026, 8).atDay(1).atStartOfDay(SEOUL).toInstant();
        Instant until = YearMonth.of(2026, 9).atDay(1).atStartOfDay(SEOUL).toInstant();
        return new RiskAlertQuery(
                USER_ID, from, until, minimumLevel, cursorAt, cursorId, limit
        );
    }

    private UUID insertParsedPayment(String occurredAt, String riskLevel, String reason) {
        UUID eventId = UUID.randomUUID();
        OffsetDateTime timestamp = OffsetDateTime.parse(occurredAt);
        jdbcClient.sql("""
                INSERT INTO raw_spend_event
                    (id, user_id, source, deduplication_key, sanitized_message, status,
                     occurred_at, received_at)
                VALUES (:id, :userId, 'SIMULATOR', :dedupe, '테스트 위험 결제', 'ANALYZING',
                        :occurredAt, :occurredAt)
                """).param("id", eventId).param("userId", USER_ID)
                .param("dedupe", "risk-alert-" + eventId).param("occurredAt", timestamp).update();
        jdbcClient.sql("""
                INSERT INTO fast_parse_result
                    (raw_event_id, amount, transaction_type, status, category, fixed_cost,
                     risk_level, risk_reason, parser_version, parsed_at)
                VALUES (:eventId, 28000, 'PAYMENT', 'PARSED', 'TRANSPORT', false,
                        :riskLevel, :reason, 'fast-parser-v1', :parsedAt)
                """).param("eventId", eventId).param("riskLevel", riskLevel)
                .param("reason", reason).param("parsedAt", timestamp).update();
        return eventId;
    }

    private void insertOverride(UUID eventId, String riskLevel, String reason) {
        jdbcClient.sql("""
                INSERT INTO spend_category_override
                    (spend_event_id, user_id, category, fixed_cost, risk_level, risk_reason,
                     version, corrected_at)
                VALUES (:eventId, :userId, 'SHOPPING', false, :riskLevel, :reason, 1, :now)
                """).param("eventId", eventId).param("userId", USER_ID)
                .param("riskLevel", riskLevel).param("reason", reason)
                .param("now", OffsetDateTime.parse("2026-08-20T00:00:00Z")).update();
    }
}
