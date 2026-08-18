package com.joajy.spendingguard.analysis.domain.model;

import java.math.BigDecimal;

/**
 * 외부 AI 호출 전에 결정론적 규칙으로 얻은 금액·거래유형 분석 결과다.
 *
 * <p>실패를 예외로 숨기지 않고 {@link FastParseStatus#NEEDS_REVIEW}와 안정적인 사유
 * 코드로 남긴다. 따라서 후속 분석이 실패해도 사용자는 검토가 필요한 이벤트를 잃지 않는다.
 *
 * @param amount 추출한 금액, 검토가 필요하면 {@code null}일 수 있음
 * @param transactionType 추출한 거래유형, 검토가 필요하면 {@code null}일 수 있음
 * @param status 빠른 분석 결과 상태
 * @param reviewReason 검토 사유 코드, 정상 분석이면 {@code null}
 */
public record FastParseOutcome(
        BigDecimal amount,
        TransactionType transactionType,
        FastParseStatus status,
        String reviewReason
) {

    public FastParseOutcome {
        if (status == null) {
            throw new IllegalArgumentException("빠른 분석 상태가 필요합니다.");
        }
        if (amount != null && amount.signum() <= 0) {
            throw new IllegalArgumentException("소비 금액은 0보다 커야 합니다.");
        }
        if (status == FastParseStatus.PARSED && (amount == null || transactionType == null)) {
            throw new IllegalArgumentException("분석 완료 결과에는 금액과 거래유형이 필요합니다.");
        }
        if (status == FastParseStatus.PARSED && reviewReason != null) {
            throw new IllegalArgumentException("분석 완료 결과에는 검토 사유를 둘 수 없습니다.");
        }
        if (status == FastParseStatus.NEEDS_REVIEW
                && (reviewReason == null || reviewReason.isBlank())) {
            throw new IllegalArgumentException("검토 필요 결과에는 사유가 필요합니다.");
        }
    }

    public static FastParseOutcome parsed(BigDecimal amount, TransactionType transactionType) {
        return new FastParseOutcome(amount, transactionType, FastParseStatus.PARSED, null);
    }

    public static FastParseOutcome needsReview(
            BigDecimal amount,
            TransactionType transactionType,
            String reason
    ) {
        return new FastParseOutcome(amount, transactionType, FastParseStatus.NEEDS_REVIEW, reason);
    }

    public boolean needsReview() {
        return status == FastParseStatus.NEEDS_REVIEW;
    }
}
