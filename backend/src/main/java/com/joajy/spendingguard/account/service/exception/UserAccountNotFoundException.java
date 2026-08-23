package com.joajy.spendingguard.account.service.exception;

public class UserAccountNotFoundException extends RuntimeException {
    public UserAccountNotFoundException() {
        super("User account was not found");
    }
}
