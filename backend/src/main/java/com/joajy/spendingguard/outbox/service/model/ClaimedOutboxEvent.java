package com.joajy.spendingguard.outbox.service.model;

import java.util.UUID;

/**
 * 한 발행 작업자가 제한된 시간 동안 처리 권한을 확보한 Outbox 이벤트다.
 *
 * <p>영속성 모델과 분리된 애플리케이션 모델이다. {@code claimToken}은 발행 결과를
 * 기록하는 작업자가 현재 임대의 소유자인지 확인하는 낙관적 소유권 표식으로 사용한다.
 *
 * @param id 이벤트 행 식별자
 * @param aggregateId 동일 소비 이벤트의 메시지 순서를 묶는 집계 식별자
 * @param eventType 소비자가 역직렬화할 이벤트 종류
 * @param payload 외부 브로커로 전달할 JSON 문자열
 * @param attemptCount 이번 선점 이전까지 실패한 발행 횟수
 * @param claimToken 현재 처리 임대를 식별하는 일회성 토큰
 */
public record ClaimedOutboxEvent(
        UUID id,
        UUID aggregateId,
        String eventType,
        String payload,
        int attemptCount,
        UUID claimToken
) {
}
