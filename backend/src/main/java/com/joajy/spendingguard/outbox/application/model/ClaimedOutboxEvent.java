package com.joajy.spendingguard.outbox.application.model;

import java.util.UUID;

public record ClaimedOutboxEvent(
        UUID id,
        UUID aggregateId,
        String eventType,
        String payload,
        int attemptCount,
        UUID claimToken
) {
}

