package com.joajy.spendingguard.account.application.exception;

public class UserAccountNotFoundException extends RuntimeException {
    public UserAccountNotFoundException() {
        super("User account was not found");
    }
}
