package com.joajy.spendingguard.spendevent.service.exception;

public class SpendEventOwnerNotFoundException extends RuntimeException {
    public SpendEventOwnerNotFoundException() { super("Spend event owner was not found"); }
}
