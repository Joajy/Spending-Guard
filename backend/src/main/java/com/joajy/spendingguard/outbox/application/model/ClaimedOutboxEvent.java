package com.joajy.spendingguard.outbox.application.model;

import java.util.UUID;

/**
 * 한 발행 작업자가 제한된 시간 동안 처리 권한을 확보한 Outbox 이벤트다.
 * 영속성 모델과 분리된 애플리케이션 모델이며, claim token을 함께 전달해 상태 변경 주체를 검증한다.
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
