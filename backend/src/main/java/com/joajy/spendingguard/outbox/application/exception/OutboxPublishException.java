package com.joajy.spendingguard.outbox.application.exception;

/**
 * 외부 메시지 브로커로 이벤트를 발행하지 못했을 때 사용하는 애플리케이션 예외다.
 * 인프라 예외를 안정적인 오류 코드로 변환해 재시도 상태에는 원인 분류만 남기고 구현 세부 정보는 노출하지 않는다.
 */
public class OutboxPublishException extends RuntimeException {

    private final String errorCode;

    public OutboxPublishException(String errorCode, Throwable cause) {
        super(errorCode, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
