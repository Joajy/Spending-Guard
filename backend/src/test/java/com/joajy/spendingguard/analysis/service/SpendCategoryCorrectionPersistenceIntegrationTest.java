package com.joajy.spendingguard.analysis.service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.SpendCategory;
import com.joajy.spendingguard.analysis.repository.FastParseResultJpaRepository;
import com.joajy.spendingguard.analysis.repository.ProcessedEventJpaRepository;
import com.joajy.spendingguard.analysis.service.command.CorrectSpendCategoryCommand;
import com.joajy.spendingguard.analysis.service.command.ProcessSpendEventCommand;
import com.joajy.spendingguard.analysis.service.exception.SpendCategoryCorrectionNotFoundException;
import com.joajy.spendingguard.analysis.service.exception.SpendCategoryVersionConflictException;
import com.joajy.spendingguard.dashboard.service.MonthlyDashboardService;
import com.joajy.spendingguard.outbox.repository.OutboxEventJpaRepository;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.repository.RawSpendEventJpaRepository;
import com.joajy.spendingguard.spendevent.service.SpendEventHistoryService;
import com.joajy.spendingguard.spendevent.service.SpendEventQueryService;
import com.joajy.spendingguard.spendevent.service.SpendEventService;
import com.joajy.spendingguard.spendevent.service.command.SubmitSpendEventCommand;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spending-guard.outbox.publisher.enabled=false",
        "spending-guard.analysis.consumer.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class SpendCategoryCorrectionPersistenceIntegrationTest {

    private static final UUID OWNER_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SpendCategoryCorrectionService correctionService;

    @Autowired
    private SpendEventProcessingService processingService;

    @Autowired
    private SpendEventService spendEventService;

    @Autowired
    private SpendEventQueryService queryService;

    @Autowired
    private SpendEventHistoryService historyService;

    @Autowired
    private MonthlyDashboardService dashboardService;

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
        jdbcClient.sql("DELETE FROM spend_category_override").update();
        fastParseResultRepository.deleteAll();
        processedEventRepository.deleteAll();
        outboxEventRepository.deleteAll();
        rawSpendEventRepository.deleteAll();
        jdbcClient.sql("DELETE FROM user_account").update();
        insertUser(OWNER_ID, "category-owner@example.com");
        insertUser(OTHER_ID, "category-other@example.com");
    }

    @Test
    void preservesOriginalCategoryAndExposesVersionedEffectiveCategory() {
        var receipt = spendEventService.submit(new SubmitSpendEventCommand(
                OWNER_ID,
                SpendEventSource.SIMULATOR,
                "category-correction-event",
                "쿠팡 60,000원 결제",
                Instant.parse("2026-08-25T00:30:00Z")
        ));
        processingService.process(new ProcessSpendEventCommand(receipt.eventId()));

        var first = correctionService.correct(new CorrectSpendCategoryCommand(
                OWNER_ID, receipt.eventId(), SpendCategory.DELIVERY, 0
        ));
        var detail = queryService.get(OWNER_ID, receipt.eventId());
        var history = historyService.list(
                OWNER_ID, YearMonth.of(2026, 8), null, "DELIVERY", null, 20
        );
        var dashboard = dashboardService.get(OWNER_ID, YearMonth.of(2026, 8));
        var storedAutomaticResult = fastParseResultRepository.findById(receipt.eventId()).orElseThrow();

        assertThat(first.originalCategory()).isEqualTo(SpendCategory.SHOPPING);
        assertThat(first.category()).isEqualTo(SpendCategory.DELIVERY);
        assertThat(first.riskLevel().name()).isEqualTo("MEDIUM");
        assertThat(first.riskReason()).isEqualTo("HIGH_DELIVERY_AMOUNT");
        assertThat(first.version()).isEqualTo(1);
        assertThat(storedAutomaticResult.getCategory()).isEqualTo(SpendCategory.SHOPPING);
        assertThat(detail.fastParse().category()).isEqualTo("DELIVERY");
        assertThat(detail.fastParse().riskLevel()).isEqualTo("MEDIUM");
        assertThat(detail.fastParse().categoryVersion()).isEqualTo(1);
        assertThat(history.items()).singleElement().satisfies(item -> {
            assertThat(item.eventId()).isEqualTo(receipt.eventId());
            assertThat(item.category()).isEqualTo("DELIVERY");
            assertThat(item.categoryVersion()).isEqualTo(1);
        });
        assertThat(dashboard.categories()).singleElement().satisfies(category -> {
            assertThat(category.category()).isEqualTo("DELIVERY");
            assertThat(category.amount()).isEqualTo(60_000);
        });
        assertThat(dashboard.risks()).singleElement().satisfies(risk -> {
            assertThat(risk.riskLevel()).isEqualTo("MEDIUM");
            assertThat(risk.count()).isEqualTo(1);
        });

        assertThatThrownBy(() -> correctionService.correct(new CorrectSpendCategoryCommand(
                OWNER_ID, receipt.eventId(), SpendCategory.OTHER, 0
        ))).isInstanceOf(SpendCategoryVersionConflictException.class);

        var second = correctionService.correct(new CorrectSpendCategoryCommand(
                OWNER_ID, receipt.eventId(), SpendCategory.SUBSCRIPTION, 1
        ));
        assertThat(second.version()).isEqualTo(2);
        assertThat(second.fixedCost()).isTrue();
        assertThat(queryService.get(OWNER_ID, receipt.eventId()).fastParse().category())
                .isEqualTo("SUBSCRIPTION");

        assertThatThrownBy(() -> correctionService.correct(new CorrectSpendCategoryCommand(
                OTHER_ID, receipt.eventId(), SpendCategory.OTHER, 2
        ))).isInstanceOf(SpendCategoryCorrectionNotFoundException.class);
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
