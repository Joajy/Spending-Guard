package com.joajy.spendingguard.spendevent.api;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.application.SpendEventReceipt;
import com.joajy.spendingguard.spendevent.domain.SpendEventStatus;

public record SpendEventAcceptedResponse(
        UUID eventId,
        SpendEventStatus status,
        Instant receivedAt
) {

    static SpendEventAcceptedResponse from(SpendEventReceipt receipt) {
        return new SpendEventAcceptedResponse(receipt.eventId(), receipt.status(), receipt.receivedAt());
    }
}
