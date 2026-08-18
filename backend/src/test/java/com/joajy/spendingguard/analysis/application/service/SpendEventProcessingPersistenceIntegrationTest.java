package com.joajy.spendingguard.analysis.application.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.joajy.spendingguard.analysis.application.command.ProcessSpendEventCommand;
import com.joajy.spendingguard.analysis.application.result.SpendEventProcessingResult;
import com.joajy.spendingguard.analysis.domain.model.FastParseStatus;
import com.joajy.spendingguard.analysis.domain.model.TransactionType;
import com.joajy.spendingguard.analysis.infrastructure.persistence.FastParseResultJpaRepository;
import com.joajy.spendingguard.analysis.infrastructure.persistence.ProcessedEventJpaRepository;
import com.joajy.spendingguard.outbox.infrastructure.persistence.OutboxEventJpaRepository;
import com.joajy.spendingguard.spendevent.application.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.application.result.SpendEventReceipt;
import com.joajy.spendingguard.spendevent.application.service.SpendEventService;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import com.joajy.spendingguard.spendevent.infrastructure.persistence.RawSpendEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spending-guard.outbox.publisher.enabled=false",
        "spending-guard.analysis.consumer.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class SpendEventProcessingPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SpendEventProcessingService processingService;

    @Autowired
    private SpendEventService spendEventService;

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
    void storesParsedFieldsAndProcessingClaimInOneTransaction() {
        SpendEventReceipt receipt = submit("쿠팡 12,800원 결제", "parse-100");

        var result = processingService.process(new ProcessSpendEventCommand(receipt.eventId()));

        var parseResult = fastParseResultRepository.findById(receipt.eventId()).orElseThrow();
        var rawEvent = rawSpendEventRepository.findById(receipt.eventId()).orElseThrow();
        assertThat(result).isEqualTo(SpendEventProcessingResult.PROCESSED);
        assertThat(parseResult.getAmount()).isEqualByComparingTo("12800");
        assertThat(parseResult.getTransactionType()).isEqualTo(TransactionType.PAYMENT);
        assertThat(parseResult.getStatus()).isEqualTo(FastParseStatus.PARSED);
        assertThat(processedEventRepository.count()).isOne();
        assertThat(rawEvent.getStatus()).isEqualTo(SpendEventStatus.ANALYZING);
    }

    @Test
    void storesReviewReasonInsteadOfGuessingMissingAmount() {
        SpendEventReceipt receipt = submit("배달의민족 결제 완료", "review-100");

        var result = processingService.process(new ProcessSpendEventCommand(receipt.eventId()));

        var parseResult = fastParseResultRepository.findById(receipt.eventId()).orElseThrow();
        var rawEvent = rawSpendEventRepository.findById(receipt.eventId()).orElseThrow();
        assertThat(result).isEqualTo(SpendEventProcessingResult.NEEDS_REVIEW);
        assertThat(parseResult.getReviewReason()).isEqualTo("AMOUNT_NOT_FOUND");
        assertThat(rawEvent.getStatus()).isEqualTo(SpendEventStatus.NEEDS_REVIEW);
    }

    @Test
    void appliesOnlyOneWriteForConcurrentRedelivery() throws Exception {
        SpendEventReceipt receipt = submit("카카오T 28,000원 승인", "redelivery-100");
        int attempts = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);

        try {
            List<Future<SpendEventProcessingResult>> futures = java.util.stream.IntStream
                    .range(0, attempts)
                    .mapToObj(ignored -> executor.submit((Callable<SpendEventProcessingResult>) () -> {
                        start.await();
                        return processingService.process(new ProcessSpendEventCommand(receipt.eventId()));
                    }))
                    .toList();
            start.countDown();

            List<SpendEventProcessingResult> results = futures.stream().map(this::get).toList();
            assertThat(results).containsOnlyOnce(SpendEventProcessingResult.PROCESSED);
            assertThat(results).filteredOn(SpendEventProcessingResult.ALREADY_PROCESSED::equals)
                    .hasSize(attempts - 1);
            assertThat(processedEventRepository.count()).isOne();
            assertThat(fastParseResultRepository.count()).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    private SpendEventReceipt submit(String message, String externalEventId) {
        return spendEventService.submit(new SubmitSpendEventCommand(
                SpendEventSource.SIMULATOR,
                externalEventId,
                message,
                Instant.parse("2026-08-18T00:59:00Z")
        ));
    }

    private SpendEventProcessingResult get(Future<SpendEventProcessingResult> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError("동시 재전달 결과를 확인할 수 없습니다.", exception);
        }
    }
}
