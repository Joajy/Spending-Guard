package com.joajy.spendingguard.analysis.service.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 빠른 분석에 필요한 원천 이벤트의 최소 데이터다.
 *
 * @param eventId 원천 이벤트 식별자
 * @param sanitizedMessage 저장 전에 민감정보 정제를 거친 알림 텍스트
 * @param occurredAt 외부 채널 기준 거래 발생 시각
 */
public record SpendEventAnalysisTarget(
        UUID eventId,
        String sanitizedMessage,
        Instant occurredAt
) {

    public SpendEventAnalysisTarget {
        Objects.requireNonNull(eventId, "소비 이벤트 식별자가 필요합니다.");
        Objects.requireNonNull(sanitizedMessage, "정제된 소비 알림이 필요합니다.");
    }
}
