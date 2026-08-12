package com.joajy.spendingguard.spendevent;

import java.time.Instant;
import java.util.UUID;

public record SpendEventAcceptedResponse(
        UUID eventId,
        SpendEventStatus status,
        Instant receivedAt
) {
}
