package com.joajy.spendingguard.outbox.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.joajy.spendingguard.outbox.service.exception.OutboxClaimLostException;
import com.joajy.spendingguard.outbox.service.model.ClaimedOutboxEvent;
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
class OutboxDispatchPersistenceIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-17T00:00:00Z");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OutboxDispatchPersistenceAdapter adapter;

    @Autowired
    private OutboxEventJpaRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void activeLeasePreventsAnotherWorkerFromClaimingSameEvent() {
        OutboxEventEntity stored = storePendingEvent();

        List<ClaimedOutboxEvent> firstClaim = adapter.claim(1, NOW, NOW.plusSeconds(30));
        List<ClaimedOutboxEvent> secondClaim = adapter.claim(1, NOW.plusSeconds(1), NOW.plusSeconds(31));

        assertThat(firstClaim).singleElement()
                .extracting(ClaimedOutboxEvent::id)
                .isEqualTo(stored.getId());
        assertThat(secondClaim).isEmpty();
    }

    @Test
    void claimsOnlyConfiguredBatchAndUsesOneLeaseToken() {
        storePendingEvent();
        storePendingEvent();
        storePendingEvent();

        List<ClaimedOutboxEvent> firstBatch = adapter.claim(2, NOW, NOW.plusSeconds(30));
        List<ClaimedOutboxEvent> secondBatch = adapter.claim(2, NOW, NOW.plusSeconds(30));

        assertThat(firstBatch).hasSize(2);
        assertThat(firstBatch)
                .extracting(ClaimedOutboxEvent::claimToken)
                .containsOnly(firstBatch.getFirst().claimToken());
        assertThat(secondBatch).hasSize(1);
        assertThat(secondBatch)
                .extracting(ClaimedOutboxEvent::id)
                .doesNotContainAnyElementsOf(
                        firstBatch.stream().map(ClaimedOutboxEvent::id).toList()
                );
    }

    @Test
    void expiredLeaseCanBeClaimedByAnotherWorker() {
        storePendingEvent();
        ClaimedOutboxEvent firstClaim = adapter.claim(1, NOW, NOW.plusSeconds(10)).getFirst();

        ClaimedOutboxEvent reclaimed = adapter.claim(
                1,
                NOW.plusSeconds(11),
                NOW.plusSeconds(41)
        ).getFirst();

        assertThat(reclaimed.id()).isEqualTo(firstClaim.id());
        assertThat(reclaimed.claimToken()).isNotEqualTo(firstClaim.claimToken());
    }

    @Test
    void failedEventWaitsUntilNextAttemptAndKeepsFailureMetadata() {
        storePendingEvent();
        ClaimedOutboxEvent claimed = adapter.claim(1, NOW, NOW.plusSeconds(30)).getFirst();
        Instant nextAttemptAt = NOW.plusSeconds(20);

        adapter.markFailed(
                claimed.id(),
                claimed.claimToken(),
                nextAttemptAt,
                "KAFKA_SEND_FAILED"
        );

        OutboxEventEntity failed = repository.findById(claimed.id()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo("PENDING");
        assertThat(failed.getAttemptCount()).isOne();
        assertThat(failed.getNextAttemptAt()).isEqualTo(nextAttemptAt);
        assertThat(failed.getLastErrorCode()).isEqualTo("KAFKA_SEND_FAILED");
        assertThat(adapter.claim(1, nextAttemptAt.minusMillis(1), nextAttemptAt.plusSeconds(30)))
                .isEmpty();
        assertThat(adapter.claim(1, nextAttemptAt, nextAttemptAt.plusSeconds(30)))
                .hasSize(1);
    }

    @Test
    void publishedEventLeavesQueueAndRejectsStaleClaimToken() {
        storePendingEvent();
        ClaimedOutboxEvent claimed = adapter.claim(1, NOW, NOW.plusSeconds(30)).getFirst();
        Instant publishedAt = NOW.plusSeconds(1);

        assertThatThrownBy(() -> adapter.markPublished(
                claimed.id(),
                UUID.randomUUID(),
                publishedAt
        )).isInstanceOf(OutboxClaimLostException.class);

        adapter.markPublished(claimed.id(), claimed.claimToken(), publishedAt);

        OutboxEventEntity published = repository.findById(claimed.id()).orElseThrow();
        assertThat(published.getStatus()).isEqualTo("PUBLISHED");
        assertThat(published.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(published.getClaimToken()).isNull();
        assertThat(published.getClaimedUntil()).isNull();
        assertThat(adapter.claim(1, NOW.plusSeconds(60), NOW.plusSeconds(90))).isEmpty();
    }

    private OutboxEventEntity storePendingEvent() {
        return repository.saveAndFlush(new OutboxEventEntity(
                UUID.randomUUID(),
                "{\"schemaVersion\":1}",
                NOW
        ));
    }
}
