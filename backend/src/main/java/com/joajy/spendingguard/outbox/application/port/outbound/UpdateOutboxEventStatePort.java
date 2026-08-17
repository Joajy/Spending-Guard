package com.joajy.spendingguard.outbox.application.port.outbound;

import java.time.Instant;
import java.util.UUID;

public interface UpdateOutboxEventStatePort {

    void markPublished(UUID eventId, UUID claimToken, Instant publishedAt);

    void markFailed(
            UUID eventId,
            UUID claimToken,
            Instant nextAttemptAt,
            String errorCode
    );
}

