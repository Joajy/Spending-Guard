package com.joajy.spendingguard.analysis.service.exception;

import java.util.UUID;

/** Kafka 이벤트가 가리키는 원천 소비 이벤트가 존재하지 않을 때 발생한다. */
public class SpendEventNotFoundException extends RuntimeException {

    public SpendEventNotFoundException(UUID eventId) {
        super("분석할 소비 이벤트를 찾을 수 없습니다: " + eventId);
    }
}
