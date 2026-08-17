package com.joajy.spendingguard.outbox.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.joajy.spendingguard.outbox.application.exception.OutboxPublishException;
import com.joajy.spendingguard.outbox.application.model.ClaimedOutboxEvent;
import com.joajy.spendingguard.outbox.application.port.outbound.ClaimOutboxEventsPort;
import com.joajy.spendingguard.outbox.application.port.outbound.PublishOutboxEventPort;
import com.joajy.spendingguard.outbox.application.port.outbound.UpdateOutboxEventStatePort;
import com.joajy.spendingguard.outbox.application.result.OutboxPublishBatchResult;
import org.springframework.stereotype.Service;

@Service
public class OutboxPublishService {

    private final ClaimOutboxEventsPort claimOutboxEventsPort;
    private final PublishOutboxEventPort publishOutboxEventPort;
    private final UpdateOutboxEventStatePort updateOutboxEventStatePort;
    private final OutboxPublishPolicy policy;
    private final Clock clock;

    public OutboxPublishService(
            ClaimOutboxEventsPort claimOutboxEventsPort,
            PublishOutboxEventPort publishOutboxEventPort,
            UpdateOutboxEventStatePort updateOutboxEventStatePort,
            OutboxPublishPolicy policy,
            Clock clock
    ) {
        this.claimOutboxEventsPort = claimOutboxEventsPort;
        this.publishOutboxEventPort = publishOutboxEventPort;
        this.updateOutboxEventStatePort = updateOutboxEventStatePort;
        this.policy = policy;
        this.clock = clock;
    }

    public OutboxPublishBatchResult publishPending() {
        Instant claimedAt = clock.instant();
        List<ClaimedOutboxEvent> events = claimOutboxEventsPort.claim(
                policy.batchSize(),
                claimedAt,
                claimedAt.plus(policy.leaseDuration())
        );
        if (events.isEmpty()) {
            return OutboxPublishBatchResult.empty();
        }

        int published = 0;
        int failed = 0;
        for (ClaimedOutboxEvent event : events) {
            try {
                publishOutboxEventPort.publish(event);
                updateOutboxEventStatePort.markPublished(
                        event.id(),
                        event.claimToken(),
                        clock.instant()
                );
                published++;
            } catch (OutboxPublishException exception) {
                Instant failedAt = clock.instant();
                updateOutboxEventStatePort.markFailed(
                        event.id(),
                        event.claimToken(),
                        failedAt.plus(policy.retryBackoff().delayAfter(event.attemptCount())),
                        exception.getErrorCode()
                );
                failed++;
            }
        }

        return new OutboxPublishBatchResult(events.size(), published, failed);
    }
}

