package com.joajy.spendingguard.outbox.application.exception;

/**
 * 외부 메시지 브로커로 이벤트를 발행하지 못했을 때 사용하는 애플리케이션 예외다.
 *
 * <p>{@code errorCode}는 재시도 상태와 운영 지표에 저장할 안정적인 분류값이다.
 * 브로커 예외 메시지나 페이로드를 데이터베이스에 남기지 않아 구현 정보와 민감 데이터의
 * 불필요한 저장을 피한다. 현재 오류 코드는 모두 재시도 가능한 전송 실패로 취급한다.
 */
public class OutboxPublishException extends RuntimeException {

    private final String errorCode;

    /**
     * 발행 실패 분류와 원래 인프라 예외를 보존한다.
     *
     * @param errorCode 저장과 집계에 사용할 안정적인 실패 코드
     * @param cause Kafka 클라이언트가 반환한 원래 실패 원인
     */
    public OutboxPublishException(String errorCode, Throwable cause) {
        super(errorCode, cause);
        this.errorCode = errorCode;
    }

    /**
     * 재시도 상태에 저장할 실패 분류 코드를 반환한다.
     *
     * @return 인프라 구현과 분리된 실패 코드
     */
    public String getErrorCode() {
        return errorCode;
    }
}
