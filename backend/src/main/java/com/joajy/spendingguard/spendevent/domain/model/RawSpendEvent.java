package com.joajy.spendingguard.spendevent.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * 외부에서 들어온 소비 알림을 최초 접수 상태로 표현하는 도메인 모델이다.
 * 원본 메시지 대신 민감 정보가 제거된 내용과 중복 판단 키를 보관해 이후 분류·위험 분석의 입력으로 사용한다.
 */
public record RawSpendEvent(
        UUID id,
        SpendEventSource source,
        String externalEventId,
        String deduplicationKey,
        String sanitizedMessage,
        SpendEventStatus status,
        Instant occurredAt,
        Instant receivedAt
) {
}
