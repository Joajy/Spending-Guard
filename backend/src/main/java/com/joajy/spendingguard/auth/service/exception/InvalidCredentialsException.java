package com.joajy.spendingguard.auth.service.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Email or password is incorrect");
    }
}
