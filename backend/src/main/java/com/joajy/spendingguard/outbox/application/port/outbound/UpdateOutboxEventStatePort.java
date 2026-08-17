package com.joajy.spendingguard.outbox.application.port.outbound;

import java.time.Instant;
import java.util.UUID;

/**
 * 발행 결과에 따라 Outbox 이벤트의 상태를 확정하는 출력 포트다.
 * 이벤트 식별자와 claim token을 함께 받아 현재 처리 권한을 가진 작업자만 성공 또는 재시도 상태를 기록하게 한다.
 */
public interface UpdateOutboxEventStatePort {

    void markPublished(UUID eventId, UUID claimToken, Instant publishedAt);

    void markFailed(
            UUID eventId,
            UUID claimToken,
            Instant nextAttemptAt,
            String errorCode
    );
}
