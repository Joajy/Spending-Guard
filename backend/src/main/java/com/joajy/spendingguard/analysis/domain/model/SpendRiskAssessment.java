package com.joajy.spendingguard.analysis.domain.model;

import java.util.Objects;

/** 규칙 기반 소비 분류와 사용자가 확인할 위험 신호다. */
public record SpendRiskAssessment(
        SpendCategory category,
        boolean fixedCost,
        RiskLevel riskLevel,
        String reasonCode
) {
    public SpendRiskAssessment {
        Objects.requireNonNull(category, "소비 카테고리가 필요합니다.");
        Objects.requireNonNull(riskLevel, "위험 수준이 필요합니다.");
        Objects.requireNonNull(reasonCode, "판정 사유가 필요합니다.");
    }
}
