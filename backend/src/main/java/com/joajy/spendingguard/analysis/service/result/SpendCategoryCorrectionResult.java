package com.joajy.spendingguard.analysis.service.result;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.analysis.domain.model.SpendCategory;

/** 자동 분류 원본과 사용자 수정 이후의 유효 분류를 함께 반환한다. */
public record SpendCategoryCorrectionResult(
        UUID eventId,
        SpendCategory originalCategory,
        SpendCategory category,
        boolean fixedCost,
        RiskLevel riskLevel,
        String riskReason,
        long version,
        Instant correctedAt
) {
}
