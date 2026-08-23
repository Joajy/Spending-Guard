package com.joajy.spendingguard.account.service.exception;

public class InvalidVerificationCodeException extends RuntimeException {
    public InvalidVerificationCodeException() {
        super("Verification code is invalid or expired");
    }
}
