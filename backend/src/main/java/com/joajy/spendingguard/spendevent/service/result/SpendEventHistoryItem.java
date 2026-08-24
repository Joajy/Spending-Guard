package com.joajy.spendingguard.spendevent.service.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;

public record SpendEventHistoryItem(
        UUID eventId,
        String displayText,
        SpendEventStatus status,
        Instant transactionAt,
        BigDecimal amount,
        String transactionType,
        String category,
        Boolean fixedCost,
        String riskLevel
) {
}
