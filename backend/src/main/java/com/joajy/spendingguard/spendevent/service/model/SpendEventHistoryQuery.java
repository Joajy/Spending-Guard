package com.joajy.spendingguard.spendevent.service.model;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;

public record SpendEventHistoryQuery(
        UUID userId,
        Instant from,
        Instant until,
        SpendEventStatus status,
        String category,
        Instant cursorTransactionAt,
        UUID cursorEventId,
        int limit
) {
}
