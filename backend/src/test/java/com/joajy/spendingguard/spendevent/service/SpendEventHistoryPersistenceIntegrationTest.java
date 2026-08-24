package com.joajy.spendingguard.spendevent.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.UUID;

import com.joajy.spendingguard.analysis.repository.FastParseResultJpaRepository;
import com.joajy.spendingguard.analysis.repository.ProcessedEventJpaRepository;
import com.joajy.spendingguard.analysis.service.SpendEventProcessingService;
import com.joajy.spendingguard.analysis.service.command.ProcessSpendEventCommand;
import com.joajy.spendingguard.outbox.repository.OutboxEventJpaRepository;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import com.joajy.spendingguard.spendevent.repository.RawSpendEventJpaRepository;
import com.joajy.spendingguard.spendevent.service.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.service.result.SpendEventReceipt;
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
class SpendEventHistoryPersistenceIntegrationTest {

    private static final UUID OWNER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SpendEventHistoryService historyService;

    @Autowired
    private SpendEventService spendEventService;

    @Autowired
    private SpendEventProcessingService processingService;

    @Autowired
    private FastParseResultJpaRepository fastParseResultRepository;

    @Autowired
    private ProcessedEventJpaRepository processedEventRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventRepository;

    @Autowired
    private RawSpendEventJpaRepository rawSpendEventRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanDatabase() {
        fastParseResultRepository.deleteAll();
        processedEventRepository.deleteAll();
        outboxEventRepository.deleteAll();
        rawSpendEventRepository.deleteAll();
        jdbcClient.sql("DELETE FROM user_account").update();
        insertUser(OWNER_ID, "history-owner@example.com");
        insertUser(OTHER_ID, "history-other@example.com");
    }

    @Test
    void filtersByOwnerMonthStatusAndCategoryWithStableCursorPaging() {
        SpendEventReceipt newest = submit(
                OWNER_ID, "카카오T 28,000원 결제", "history-owner-newest",
                "2026-08-18T01:00:00Z"
        );
        SpendEventReceipt older = submit(
                OWNER_ID, "쿠팡 12,800원 결제", "history-owner-older",
                "2026-08-17T01:00:00Z"
        );
        submit(
                OTHER_ID, "카카오T 31,000원 결제", "history-other",
                "2026-08-18T02:00:00Z"
        );
        submit(
                OWNER_ID, "배민 22,000원 결제", "history-next-month",
                "2026-08-31T15:00:00Z"
        );
        processingService.process(new ProcessSpendEventCommand(newest.eventId()));

        var full = historyService.list(
                OWNER_ID, YearMonth.of(2026, 8), null, null, null, 100
        );
        var first = historyService.list(
                OWNER_ID, YearMonth.of(2026, 8), null, null, null, 1
        );
        var second = historyService.list(
                OWNER_ID, YearMonth.of(2026, 8), null, null, first.nextCursor(), 1
        );
        var transport = historyService.list(
                OWNER_ID, YearMonth.of(2026, 8), null, "TRANSPORT", null, 20
        );
        var waiting = historyService.list(
                OWNER_ID, YearMonth.of(2026, 8), SpendEventStatus.RECEIVED, null, null, 20
        );

        assertThat(full.items()).extracting(item -> item.eventId())
                .containsExactly(newest.eventId(), older.eventId());
        assertThat(first.hasNext()).isTrue();
        assertThat(first.nextCursor()).isNotBlank();
        assertThat(first.items().get(0).eventId()).isEqualTo(newest.eventId());
        assertThat(second.hasNext()).isFalse();
        assertThat(second.items().get(0).eventId()).isEqualTo(older.eventId());
        assertThat(transport.items()).singleElement().satisfies(item -> {
            assertThat(item.eventId()).isEqualTo(newest.eventId());
            assertThat(item.category()).isEqualTo("TRANSPORT");
            assertThat(item.amount()).isEqualByComparingTo("28000");
        });
        assertThat(waiting.items()).extracting(item -> item.eventId())
                .containsExactly(older.eventId());
    }

    private SpendEventReceipt submit(
            UUID userId,
            String message,
            String externalEventId,
            String occurredAt
    ) {
        return spendEventService.submit(new SubmitSpendEventCommand(
                userId,
                SpendEventSource.SIMULATOR,
                externalEventId,
                message,
                Instant.parse(occurredAt)
        ));
    }

    private void insertUser(UUID id, String email) {
        jdbcClient.sql("""
                        INSERT INTO user_account (id, email, password_hash, created_at, email_verified_at)
                        VALUES (:id, :email, :passwordHash, :createdAt, :verifiedAt)
                        """)
                .param("id", id)
                .param("email", email)
                .param("passwordHash", "integration-test-password-hash")
                .param("createdAt", Instant.parse("2026-08-01T00:00:00Z").atOffset(ZoneOffset.UTC))
                .param("verifiedAt", Instant.parse("2026-08-01T00:01:00Z").atOffset(ZoneOffset.UTC))
                .update();
    }
}
