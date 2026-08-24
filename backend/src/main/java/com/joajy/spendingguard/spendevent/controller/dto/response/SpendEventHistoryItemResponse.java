package com.joajy.spendingguard.spendevent.controller.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import com.joajy.spendingguard.spendevent.service.result.SpendEventHistoryItem;

public record SpendEventHistoryItemResponse(
        UUID eventId,
        String displayText,
        SpendEventStatus status,
        Instant transactionAt,
        BigDecimal amount,
        String currency,
        String transactionType,
        String category,
        Boolean fixedCost,
        String riskLevel,
        long categoryVersion
) {
    static SpendEventHistoryItemResponse from(SpendEventHistoryItem item) {
        return new SpendEventHistoryItemResponse(
                item.eventId(),
                item.displayText(),
                item.status(),
                item.transactionAt(),
                item.amount(),
                item.amount() == null ? null : "KRW",
                item.transactionType(),
                item.category(),
                item.fixedCost(),
                item.riskLevel(),
                item.categoryVersion()
        );
    }
}
