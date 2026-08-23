package com.joajy.spendingguard.auth.service.exception;

public class UnverifiedEmailException extends RuntimeException {
    public UnverifiedEmailException() {
        super("Email verification is required");
    }
}
