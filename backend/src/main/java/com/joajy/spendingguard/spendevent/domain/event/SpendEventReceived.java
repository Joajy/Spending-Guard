package com.joajy.spendingguard.spendevent.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;

public record SpendEventReceived(
        UUID eventId,
        SpendEventSource source,
        Instant receivedAt
) {
}
