package com.joajy.spendingguard.riskalert.service.model;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.RiskLevel;

public record RiskAlertQuery(
        UUID userId,
        Instant from,
        Instant until,
        RiskLevel minimumLevel,
        Instant cursorTransactionAt,
        UUID cursorEventId,
        int limit
) {
}
