package com.joajy.spendingguard.spendevent.application.result;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;

public record SpendEventReceipt(
        UUID eventId,
        SpendEventStatus status,
        Instant receivedAt
) {
}
