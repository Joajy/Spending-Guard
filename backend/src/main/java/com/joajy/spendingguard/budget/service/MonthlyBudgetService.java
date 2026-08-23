package com.joajy.spendingguard.budget.service;

import java.time.Clock;
import java.time.YearMonth;
import java.util.UUID;
import com.joajy.spendingguard.account.service.exception.UserAccountNotFoundException;
import com.joajy.spendingguard.budget.domain.MonthlyBudget;
import com.joajy.spendingguard.budget.service.port.MonthlyBudgetStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonthlyBudgetService {
    private final MonthlyBudgetStore store;
    private final Clock clock;

    public MonthlyBudgetService(MonthlyBudgetStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MonthlyBudget get(UUID userId, YearMonth month) {
        return store.find(userId, month).orElseThrow(BudgetNotFoundException::new);
    }

    @Transactional
    public MonthlyBudget set(UUID userId, YearMonth month, long amount, Long expectedVersion) {
        if (!store.accountExists(userId)) throw new UserAccountNotFoundException();
        var current = store.find(userId, month);
        if (current.isEmpty()) {
            if (expectedVersion != null) throw new StaleBudgetVersionException();
            return store.create(userId, month, amount, clock.instant());
        }
        if (expectedVersion == null || current.get().version() != expectedVersion) {
            throw new StaleBudgetVersionException();
        }
        return store.update(current.get(), amount, clock.instant());
    }
}
