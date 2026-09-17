package com.joajy.spendingguard.spendevent.service;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.analysis.service.command.ProcessSpendEventCommand;
import com.joajy.spendingguard.analysis.service.SpendEventProcessingService;
import com.joajy.spendingguard.analysis.repository.FastParseResultJpaRepository;
import com.joajy.spendingguard.analysis.repository.ProcessedEventJpaRepository;
import com.joajy.spendingguard.outbox.repository.OutboxEventJpaRepository;
import com.joajy.spendingguard.spendevent.service.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.service.exception.SpendEventNotFoundException;
import com.joajy.spendingguard.spendevent.service.result.SpendEventDetail;
import com.joajy.spendingguard.spendevent.service.result.SpendEventReceipt;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import com.joajy.spendingguard.spendevent.repository.RawSpendEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
class SpendEventQueryPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SpendEventQueryService queryService;

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

    @BeforeEach
    void cleanDatabase() {
        fastParseResultRepository.deleteAll();
        processedEventRepository.deleteAll();
        outboxEventRepository.deleteAll();
        rawSpendEventRepository.deleteAll();
    }

    @Test
    void returnsReceivedEventBeforeAnalysisStarts() {
        SpendEventReceipt receipt = submit("토스페이 9,900원 결제", "query-received-100");

        SpendEventDetail detail = queryService.get(receipt.eventId());

        assertThat(detail.status()).isEqualTo(SpendEventStatus.RECEIVED);
        assertThat(detail.source()).isEqualTo(SpendEventSource.SIMULATOR);
        assertThat(detail.fastParse()).isNull();
    }

    @Test
    void joinsFastParseResultAfterAnalysis() {
        SpendEventReceipt receipt = submit("카카오T 28,000원 결제", "query-parsed-100");
        processingService.process(new ProcessSpendEventCommand(receipt.eventId()));

        SpendEventDetail detail = queryService.get(receipt.eventId());

        assertThat(detail.status()).isEqualTo(SpendEventStatus.COMPLETED);
        assertThat(detail.fastParse()).isNotNull();
        assertThat(detail.fastParse().amount()).isEqualByComparingTo("28000");
        assertThat(detail.fastParse().transactionType()).isEqualTo("PAYMENT");
        assertThat(detail.fastParse().status()).isEqualTo("PARSED");
        assertThat(detail.fastParse().parserVersion()).isEqualTo("fast-parser-v1");
    }

    @Test
    void rejectsUnknownEvent() {
        UUID eventId = UUID.fromString("b894a4c7-0c4b-453b-8c82-92bbcd6bd8eb");

        assertThatThrownBy(() -> queryService.get(eventId))
                .isInstanceOf(SpendEventNotFoundException.class);
    }

    private SpendEventReceipt submit(String message, String externalEventId) {
        return spendEventService.submit(new SubmitSpendEventCommand(
                SpendEventSource.SIMULATOR,
                externalEventId,
                message,
                Instant.parse("2026-08-18T01:00:00Z")
        ));
    }
}
