package com.joajy.spendingguard.spendevent.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * 외부에서 들어온 소비 알림을 최초 접수 상태로 표현하는 도메인 모델이다.
 *
 * <p>원본 메시지 대신 민감 정보가 제거된 내용과 중복 판단 키를 보관한다. 발생 시각과
 * 접수 시각을 구분해 지연 유입을 계산할 수 있고, 후속 분류·위험 분석은 이 모델을 입력으로 사용한다.
 *
 * @param id 서버가 생성한 이벤트 식별자
 * @param source 이벤트가 유입된 채널
 * @param externalEventId 채널이 제공한 원본 이벤트 식별자
 * @param deduplicationKey 동일 알림의 재접수를 막는 SHA-256 키
 * @param sanitizedMessage 이메일과 긴 숫자열을 제거한 분석용 텍스트
 * @param status 현재 처리 상태
 * @param occurredAt 외부 채널 기준 발생 시각
 * @param receivedAt 서버 기준 접수 시각
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
