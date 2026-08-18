package com.joajy.spendingguard.spendevent.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.joajy.spendingguard.spendevent.application.result.FastParseResult;

/**
 * 외부 분석 전에 규칙으로 확보한 최소 거래 정보를 표현한다.
 *
 * <p>금액을 확정할 수 없는 경우에는 {@code amount}와 {@code transactionType}이 비어 있고
 * {@code status=NEEDS_REVIEW}와 {@code reviewReason}이 사용자의 확인 필요 사유를 설명한다.
 */
public record FastParseResultResponse(
        BigDecimal amount,
        String currency,
        String transactionType,
        String status,
        String reviewReason,
        String parserVersion,
        Instant parsedAt
) {

    static FastParseResultResponse from(FastParseResult result) {
        if (result == null) {
            return null;
        }
        return new FastParseResultResponse(
                result.amount(),
                "KRW",
                result.transactionType(),
                result.status(),
                result.reviewReason(),
                result.parserVersion(),
                result.parsedAt()
        );
    }
}

