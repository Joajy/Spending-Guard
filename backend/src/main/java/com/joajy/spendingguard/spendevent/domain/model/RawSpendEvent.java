package com.joajy.spendingguard.spendevent.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RawSpendEvent(
        UUID id,
        SpendEventSource source,
        String externalEventId,
        String deduplicationKey,
        String sanitizedMessage,
        SpendEventStatus status,
        Instant occurredAt,
        Instant receivedAt
) {
}
