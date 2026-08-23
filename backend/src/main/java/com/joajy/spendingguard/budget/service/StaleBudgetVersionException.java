package com.joajy.spendingguard.budget.service;

public class StaleBudgetVersionException extends RuntimeException {
    public StaleBudgetVersionException() { super("Budget was changed by another request"); }
}
