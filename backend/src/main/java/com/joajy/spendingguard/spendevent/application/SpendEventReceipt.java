package com.joajy.spendingguard.spendevent.application;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.SpendEventStatus;

public record SpendEventReceipt(
        UUID eventId,
        SpendEventStatus status,
        Instant receivedAt
) {
}
