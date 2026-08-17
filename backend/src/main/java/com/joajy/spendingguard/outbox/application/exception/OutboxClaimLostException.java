package com.joajy.spendingguard.outbox.application.exception;

import java.util.UUID;

/**
 * 발행 결과를 반영하려는 시점에 Outbox 이벤트의 처리 권한이 만료되었음을 알린다.
 * claim token이 일치하는 행만 변경하도록 강제해, 임대 시간이 지난 작업자가 새 작업자의 상태를 덮어쓰지 못하게 한다.
 */
public class OutboxClaimLostException extends RuntimeException {

    public OutboxClaimLostException(UUID eventId) {
        super("Outbox event claim is no longer valid: " + eventId);
    }
}
