package com.joajy.spendingguard.spendevent.api.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.application.result.SpendEventReceipt;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;

public record SpendEventAcceptedResponse(
        UUID eventId,
        SpendEventStatus status,
        Instant receivedAt
) {

    public static SpendEventAcceptedResponse from(SpendEventReceipt receipt) {
        return new SpendEventAcceptedResponse(receipt.eventId(), receipt.status(), receipt.receivedAt());
    }
}
