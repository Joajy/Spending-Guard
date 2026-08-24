package com.joajy.spendingguard.analysis.controller.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.analysis.domain.model.SpendCategory;
import com.joajy.spendingguard.analysis.service.result.SpendCategoryCorrectionResult;

public record SpendCategoryCorrectionResponse(
        UUID eventId,
        SpendCategory originalCategory,
        SpendCategory category,
        boolean fixedCost,
        RiskLevel riskLevel,
        String riskReason,
        long version,
        Instant correctedAt
) {
    public static SpendCategoryCorrectionResponse from(SpendCategoryCorrectionResult result) {
        return new SpendCategoryCorrectionResponse(
                result.eventId(),
                result.originalCategory(),
                result.category(),
                result.fixedCost(),
                result.riskLevel(),
                result.riskReason(),
                result.version(),
                result.correctedAt()
        );
    }
}
