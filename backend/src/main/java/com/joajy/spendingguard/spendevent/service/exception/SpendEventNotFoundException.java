package com.joajy.spendingguard.spendevent.service.exception;

import java.util.UUID;

/** 요청한 식별자에 해당하는 소비 이벤트가 없을 때 조회 유스케이스가 반환하는 예외다. */
public class SpendEventNotFoundException extends RuntimeException {

    public SpendEventNotFoundException(UUID eventId) {
        super("소비 이벤트를 찾을 수 없습니다: " + eventId);
    }
}

