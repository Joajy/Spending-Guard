package com.joajy.spendingguard.account.service.exception;

public class VerificationCodeRequestTooFrequentException extends RuntimeException {
    public VerificationCodeRequestTooFrequentException() {
        super("Wait 60 seconds before requesting another verification code");
    }
}
