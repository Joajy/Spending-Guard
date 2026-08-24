package com.joajy.spendingguard.spendevent.service.result;

import java.math.BigDecimal;
import java.time.Instant;

/** 조회 시점에 저장되어 있는 규칙 기반 파싱 결과의 읽기 모델이다. */
public record FastParseResult(
        BigDecimal amount,
        String transactionType,
        String status,
        String reviewReason,
        String category,
        Boolean fixedCost,
        String riskLevel,
        String riskReason,
        String parserVersion,
        Instant parsedAt,
        long categoryVersion
) {
}
