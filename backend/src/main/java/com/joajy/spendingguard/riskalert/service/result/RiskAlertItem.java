package com.joajy.spendingguard.riskalert.service.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.RiskLevel;

public record RiskAlertItem(
        UUID eventId,
        String displayText,
        Instant transactionAt,
        BigDecimal amount,
        String category,
        RiskLevel riskLevel,
        String reasonCode,
        long categoryVersion
) {
}
