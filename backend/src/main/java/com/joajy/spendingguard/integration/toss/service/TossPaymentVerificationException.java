package com.joajy.spendingguard.integration.toss.service;

/** 웹훅과 Toss Payments 원본 결제를 안전하게 대조할 수 없을 때 발생한다. */
public class TossPaymentVerificationException extends RuntimeException {

    public TossPaymentVerificationException(String message) {
        super(message);
    }

    public TossPaymentVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
