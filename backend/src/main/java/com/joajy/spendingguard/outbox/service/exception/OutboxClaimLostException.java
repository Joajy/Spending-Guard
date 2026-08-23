package com.joajy.spendingguard.outbox.service.exception;

import java.util.UUID;

/**
 * 발행 결과를 반영하려는 시점에 Outbox 이벤트의 처리 권한이 만료되었음을 알린다.
 *
 * <p>대상 행이 없다는 의미가 아니라, 상태가 더 이상 {@code PROCESSING}이 아니거나
 * claim token이 달라 현재 작업자가 갱신 권한을 잃었다는 의미다. 호출자는 이 예외를
 * 일반 발행 실패처럼 재시도 상태로 덮어쓰지 않아야 한다.
 */
public class OutboxClaimLostException extends RuntimeException {

    /**
     * 처리 권한을 잃은 이벤트의 식별자를 포함한 예외를 생성한다.
     *
     * @param eventId 상태 변경에 실패한 Outbox 이벤트 식별자
     */
    public OutboxClaimLostException(UUID eventId) {
        super("Outbox event claim is no longer valid: " + eventId);
    }
}
