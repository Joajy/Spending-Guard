package com.joajy.spendingguard.outbox.service.port.outbound;

import java.time.Instant;
import java.util.UUID;

/**
 * 발행 결과에 따라 Outbox 이벤트의 상태를 확정하는 출력 포트다.
 *
 * <p>모든 상태 변경은 이벤트 식별자와 claim token을 함께 비교해야 한다. 구현체는
 * 현재 임대가 아니면 상태를 변경하지 않고 {@code OutboxClaimLostException}을 발생시킨다.
 */
public interface UpdateOutboxEventStatePort {

    /**
     * 브로커 전송이 확인된 이벤트를 발행 완료 상태로 변경한다.
     *
     * @param eventId 상태를 변경할 Outbox 이벤트 식별자
     * @param claimToken 호출자가 보유한 처리 권한 토큰
     * @param publishedAt 브로커 발행이 확인된 시각
     */
    void markPublished(UUID eventId, UUID claimToken, Instant publishedAt);

    /**
     * 발행에 실패한 이벤트를 다음 시도 시각까지 대기 상태로 되돌린다.
     *
     * @param eventId 상태를 변경할 Outbox 이벤트 식별자
     * @param claimToken 호출자가 보유한 처리 권한 토큰
     * @param nextAttemptAt 다음 선점을 허용할 시각
     * @param errorCode 인프라 예외 대신 저장할 안정적인 실패 분류 코드
     */
    void markFailed(
            UUID eventId,
            UUID claimToken,
            Instant nextAttemptAt,
            String errorCode
    );
}
