package com.joajy.spendingguard.spendevent.application;

import java.time.Instant;

import com.joajy.spendingguard.spendevent.domain.SpendEventSource;

public record SubmitSpendEventCommand(
        SpendEventSource source,
        String externalEventId,
        String message,
        Instant occurredAt
) {
}
