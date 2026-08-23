package com.joajy.spendingguard.account.service.exception;

public class TooManyVerificationAttemptsException extends RuntimeException {
    public TooManyVerificationAttemptsException() {
        super("Request a new verification code after too many failed attempts");
    }
}
