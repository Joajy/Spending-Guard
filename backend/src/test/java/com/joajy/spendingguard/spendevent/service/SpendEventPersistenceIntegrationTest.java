package com.joajy.spendingguard.spendevent.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.joajy.spendingguard.account.repository.UserAccountJpaRepository;
import com.joajy.spendingguard.account.service.UserRegistrationService;
import com.joajy.spendingguard.account.service.command.RegisterUserCommand;
import com.joajy.spendingguard.outbox.repository.OutboxEventEntity;
import com.joajy.spendingguard.outbox.repository.OutboxEventJpaRepository;
import com.joajy.spendingguard.spendevent.service.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.service.exception.DuplicateSpendEventException;
import com.joajy.spendingguard.spendevent.service.result.SpendEventReceipt;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.repository.RawSpendEventEntity;
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
class SpendEventPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SpendEventService spendEventService;

    @Autowired
    private RawSpendEventJpaRepository rawSpendEventRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventRepository;

    @Autowired
    private UserRegistrationService registrationService;

    @Autowired
    private UserAccountJpaRepository userAccountRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        rawSpendEventRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void storesSanitizedEventAndOutboxTogether() {
        SpendEventReceipt receipt = spendEventService.submit(new SubmitSpendEventCommand(
                SpendEventSource.MANUAL_TEXT,
                null,
                "test@example.com 테스트카드 1234-5678-9012-3456 12,800원 결제",
                Instant.parse("2026-08-13T01:00:00Z")
        ));

        RawSpendEventEntity storedEvent = rawSpendEventRepository.findById(receipt.eventId()).orElseThrow();
        OutboxEventEntity storedOutbox = outboxEventRepository.findAll().getFirst();

        assertThat(storedEvent.getSanitizedMessage())
                .contains("[EMAIL]", "[REDACTED]", "12,800원")
                .doesNotContain("test@example.com", "1234-5678-9012-3456");
        assertThat(storedOutbox.getAggregateId()).isEqualTo(receipt.eventId());
        assertThat(storedOutbox.getEventType()).isEqualTo("SpendEventReceived");
        assertThat(storedOutbox.getStatus()).isEqualTo("PENDING");
        assertThat(storedOutbox.getPayload())
                .contains(receipt.eventId().toString(), "schemaVersion")
                .doesNotContain("1234-5678-9012-3456");
    }

    @Test
    void allowsIdenticalUndatedMessagesForDifferentUsers() {
        UUID firstUser = registerUser("first@example.com");
        UUID secondUser = registerUser("second@example.com");
        SubmitSpendEventCommand first = new SubmitSpendEventCommand(
                firstUser, SpendEventSource.MANUAL_TEXT, null, "쿠팡 12,800원 결제", null);
        SubmitSpendEventCommand second = new SubmitSpendEventCommand(
                secondUser, SpendEventSource.MANUAL_TEXT, null, "쿠팡 12,800원 결제", null);

        spendEventService.submit(first);
        spendEventService.submit(second);

        assertThatThrownBy(() -> spendEventService.submit(first))
                .isInstanceOf(DuplicateSpendEventException.class);
        assertThat(rawSpendEventRepository.count()).isEqualTo(2);
        assertThat(outboxEventRepository.count()).isEqualTo(2);
    }

    @Test
    void scopesExternalEventIdsToTheirOwner() {
        SubmitSpendEventCommand first = new SubmitSpendEventCommand(
                registerUser("first@example.com"),
                SpendEventSource.SIMULATOR, "shared-id", "상점 1,000원 결제", null);
        SubmitSpendEventCommand second = new SubmitSpendEventCommand(
                registerUser("second@example.com"),
                SpendEventSource.SIMULATOR, "shared-id", "상점 2,000원 결제", null);

        spendEventService.submit(first);
        spendEventService.submit(second);

        assertThatThrownBy(() -> spendEventService.submit(second))
                .isInstanceOf(DuplicateSpendEventException.class);
        assertThat(rawSpendEventRepository.count()).isEqualTo(2);
        assertThat(outboxEventRepository.count()).isEqualTo(2);
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

    private UUID registerUser(String email) {
        return registrationService.register(new RegisterUserCommand(email, "safe-password-123")).userId();
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
