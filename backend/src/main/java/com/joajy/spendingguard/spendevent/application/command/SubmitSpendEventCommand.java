package com.joajy.spendingguard.spendevent.application.command;

import java.time.Instant;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;

public record SubmitSpendEventCommand(
        SpendEventSource source,
        String externalEventId,
        String message,
        Instant occurredAt
) {
}
