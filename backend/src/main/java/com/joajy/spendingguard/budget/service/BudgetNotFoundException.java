package com.joajy.spendingguard.budget.service;

public class BudgetNotFoundException extends RuntimeException {
    public BudgetNotFoundException() { super("Monthly budget was not found"); }
}
