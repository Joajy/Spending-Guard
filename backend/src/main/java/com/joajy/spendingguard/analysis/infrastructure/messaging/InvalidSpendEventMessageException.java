package com.joajy.spendingguard.analysis.infrastructure.messaging;

/** 재시도로 복구할 수 없는 Kafka 메시지 계약 위반을 나타낸다. */
public class InvalidSpendEventMessageException extends RuntimeException {

    InvalidSpendEventMessageException(Throwable cause) {
        super("소비 이벤트 메시지 계약을 해석할 수 없습니다.", cause);
    }
}
