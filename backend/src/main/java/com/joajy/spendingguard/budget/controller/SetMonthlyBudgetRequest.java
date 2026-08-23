package com.joajy.spendingguard.budget.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SetMonthlyBudgetRequest(@Min(1) @Max(1_000_000_000_000L) long amount, Long version) { }
