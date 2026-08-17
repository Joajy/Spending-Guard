package com.joajy.spendingguard.outbox.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.joajy.spendingguard.outbox.application.exception.OutboxPublishException;
import com.joajy.spendingguard.outbox.application.model.ClaimedOutboxEvent;
import com.joajy.spendingguard.outbox.application.port.outbound.ClaimOutboxEventsPort;
import com.joajy.spendingguard.outbox.application.port.outbound.PublishOutboxEventPort;
import com.joajy.spendingguard.outbox.application.port.outbound.UpdateOutboxEventStatePort;
import com.joajy.spendingguard.outbox.application.result.OutboxPublishBatchResult;
import com.joajy.spendingguard.outbox.domain.policy.ExponentialRetryBackoff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OutboxPublishServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-17T00:00:00Z");
    private static final Duration LEASE_DURATION = Duration.ofSeconds(30);

    @Mock
    private ClaimOutboxEventsPort claimOutboxEventsPort;

    @Mock
    private PublishOutboxEventPort publishOutboxEventPort;

    @Mock
    private UpdateOutboxEventStatePort updateOutboxEventStatePort;

    private OutboxPublishService service;

    @BeforeEach
    void setUp() {
        service = new OutboxPublishService(
                claimOutboxEventsPort,
                publishOutboxEventPort,
                updateOutboxEventStatePort,
                new OutboxPublishPolicy(
                        20,
                        LEASE_DURATION,
                        new ExponentialRetryBackoff(Duration.ofSeconds(5), Duration.ofMinutes(5))
                ),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void publishesClaimedEventsAndMarksThemComplete() {
        ClaimedOutboxEvent event = claimedEvent(0);
        given(claimOutboxEventsPort.claim(20, NOW, NOW.plus(LEASE_DURATION)))
                .willReturn(List.of(event));

        OutboxPublishBatchResult result = service.publishPending();

        verify(publishOutboxEventPort).publish(event);
        verify(updateOutboxEventStatePort).markPublished(event.id(), event.claimToken(), NOW);
        assertThat(result).isEqualTo(new OutboxPublishBatchResult(1, 1, 0));
    }

    @Test
    void schedulesFailedEventForExponentialRetryAndContinuesBatch() {
        ClaimedOutboxEvent failedEvent = claimedEvent(2);
        ClaimedOutboxEvent publishedEvent = claimedEvent(0);
        given(claimOutboxEventsPort.claim(20, NOW, NOW.plus(LEASE_DURATION)))
                .willReturn(List.of(failedEvent, publishedEvent));
        willThrow(new OutboxPublishException("KAFKA_SEND_FAILED", new IllegalStateException()))
                .given(publishOutboxEventPort).publish(failedEvent);

        OutboxPublishBatchResult result = service.publishPending();

        verify(updateOutboxEventStatePort).markFailed(
                failedEvent.id(),
                failedEvent.claimToken(),
                NOW.plusSeconds(20),
                "KAFKA_SEND_FAILED"
        );
        verify(updateOutboxEventStatePort).markPublished(
                publishedEvent.id(),
                publishedEvent.claimToken(),
                NOW
        );
        assertThat(result).isEqualTo(new OutboxPublishBatchResult(2, 1, 1));
    }

    @Test
    void doesNothingWhenNoEventCanBeClaimed() {
        given(claimOutboxEventsPort.claim(20, NOW, NOW.plus(LEASE_DURATION)))
                .willReturn(List.of());

        assertThat(service.publishPending()).isEqualTo(OutboxPublishBatchResult.empty());
        verifyNoInteractions(publishOutboxEventPort, updateOutboxEventStatePort);
    }

    private ClaimedOutboxEvent claimedEvent(int attemptCount) {
        return new ClaimedOutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SpendEventReceived",
                "{\"schemaVersion\":1}",
                attemptCount,
                UUID.randomUUID()
        );
    }
}

