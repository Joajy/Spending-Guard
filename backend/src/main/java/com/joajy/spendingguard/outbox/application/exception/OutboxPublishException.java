package com.joajy.spendingguard.outbox.application.exception;

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

