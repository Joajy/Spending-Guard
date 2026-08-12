package com.joajy.spendingguard.spendevent.application;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.joajy.spendingguard.outbox.OutboxEvent;
import com.joajy.spendingguard.outbox.OutboxEventRepository;
import com.joajy.spendingguard.outbox.OutboxStatus;
import com.joajy.spendingguard.spendevent.domain.SpendEventSource;
import com.joajy.spendingguard.spendevent.persistence.RawSpendEvent;
import com.joajy.spendingguard.spendevent.persistence.RawSpendEventRepository;
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

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SpendEventPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SpendEventService spendEventService;

    @Autowired
    private RawSpendEventRepository rawSpendEventRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        rawSpendEventRepository.deleteAll();
    }

    @Test
    void storesSanitizedEventAndOutboxTogether() {
        SpendEventReceipt receipt = spendEventService.submit(new SubmitSpendEventCommand(
                SpendEventSource.MANUAL_TEXT,
                null,
                "test@example.com 테스트카드 1234-5678-9012-3456 12,800원 결제",
                Instant.parse("2026-08-13T01:00:00Z")
        ));

        RawSpendEvent storedEvent = rawSpendEventRepository.findById(receipt.eventId()).orElseThrow();
        OutboxEvent storedOutbox = outboxEventRepository.findAll().getFirst();

        assertThat(storedEvent.getSanitizedMessage())
                .contains("[EMAIL]", "[REDACTED]", "12,800원")
                .doesNotContain("test@example.com", "1234-5678-9012-3456");
        assertThat(storedOutbox.getAggregateId()).isEqualTo(receipt.eventId());
        assertThat(storedOutbox.getEventType()).isEqualTo("SpendEventReceived");
        assertThat(storedOutbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(storedOutbox.getPayload())
                .contains(receipt.eventId().toString(), "schemaVersion")
                .doesNotContain("1234-5678-9012-3456");
    }

    @Test
    void rejectsRepeatedExternalEventIdWithoutAddingRows() {
        SubmitSpendEventCommand first = new SubmitSpendEventCommand(
                SpendEventSource.SIMULATOR,
                "simulator-event-100",
                "테스트상점 12,800원 결제",
                Instant.parse("2026-08-13T01:00:00Z")
        );
        SubmitSpendEventCommand repeated = new SubmitSpendEventCommand(
                SpendEventSource.SIMULATOR,
                "simulator-event-100",
                "메시지가 달라도 외부 이벤트 ID는 동일",
                Instant.parse("2026-08-13T01:01:00Z")
        );

        spendEventService.submit(first);

        assertThatThrownBy(() -> spendEventService.submit(repeated))
                .isInstanceOf(DuplicateSpendEventException.class);
        assertThat(rawSpendEventRepository.count()).isOne();
        assertThat(outboxEventRepository.count()).isOne();
    }

    @Test
    void allowsSameMessageWhenOccurredAtIsDifferent() {
        String message = "테스트택시 18,000원 결제";

        spendEventService.submit(new SubmitSpendEventCommand(
                SpendEventSource.MANUAL_TEXT,
                null,
                message,
                Instant.parse("2026-08-13T01:00:00Z")
        ));
        spendEventService.submit(new SubmitSpendEventCommand(
                SpendEventSource.MANUAL_TEXT,
                null,
                message,
                Instant.parse("2026-08-13T02:00:00Z")
        ));

        assertThat(rawSpendEventRepository.count()).isEqualTo(2);
        assertThat(outboxEventRepository.count()).isEqualTo(2);
    }

    @Test
    void acceptsOnlyOneOfConcurrentDuplicateRequests() throws Exception {
        int attempts = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        SubmitSpendEventCommand command = new SubmitSpendEventCommand(
                SpendEventSource.SIMULATOR,
                "concurrent-event-100",
                "테스트쇼핑 31,000원 결제",
                Instant.parse("2026-08-13T03:00:00Z")
        );

        try {
            List<Future<Result>> results = java.util.stream.IntStream.range(0, attempts)
                    .mapToObj(ignored -> executor.submit((Callable<Result>) () -> submitAfter(start, command)))
                    .toList();
            start.countDown();

            List<Result> completed = results.stream().map(this::get).toList();
            assertThat(completed).containsExactlyInAnyOrderElementsOf(List.of(
                    Result.ACCEPTED,
                    Result.DUPLICATE,
                    Result.DUPLICATE,
                    Result.DUPLICATE,
                    Result.DUPLICATE,
                    Result.DUPLICATE,
                    Result.DUPLICATE,
                    Result.DUPLICATE
            ));
            assertThat(rawSpendEventRepository.count()).isOne();
            assertThat(outboxEventRepository.count()).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    private Result submitAfter(CountDownLatch start, SubmitSpendEventCommand command) throws InterruptedException {
        start.await();
        try {
            spendEventService.submit(command);
            return Result.ACCEPTED;
        } catch (DuplicateSpendEventException exception) {
            return Result.DUPLICATE;
        }
    }

    private Result get(Future<Result> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError("동시 요청 결과를 확인할 수 없습니다.", exception);
        }
    }

    private enum Result {
        ACCEPTED,
        DUPLICATE
    }
}
