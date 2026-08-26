package com.joajy.spendingguard.riskalert.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.riskalert.service.result.RiskAlertItem;

public record RiskAlertItemResponse(
        UUID eventId,
        String displayText,
        Instant transactionAt,
        BigDecimal amount,
        String category,
        RiskLevel riskLevel,
        String reasonCode,
        long categoryVersion
) {

    static RiskAlertItemResponse from(RiskAlertItem item) {
        return new RiskAlertItemResponse(
                item.eventId(),
                item.displayText(),
                item.transactionAt(),
                item.amount(),
                item.category(),
                item.riskLevel(),
                item.reasonCode(),
                item.categoryVersion()
        );
    }
}
