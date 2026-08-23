package com.joajy.spendingguard.budget.repository;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import com.joajy.spendingguard.account.repository.UserAccountJpaRepository;
import com.joajy.spendingguard.budget.domain.MonthlyBudget;
import com.joajy.spendingguard.budget.service.port.MonthlyBudgetStore;
import org.springframework.stereotype.Component;

@Component
class MonthlyBudgetPersistenceAdapter implements MonthlyBudgetStore {
    private final UserAccountJpaRepository accounts;
    private final MonthlyBudgetJpaRepository budgets;
    MonthlyBudgetPersistenceAdapter(UserAccountJpaRepository accounts, MonthlyBudgetJpaRepository budgets) {
        this.accounts=accounts; this.budgets=budgets;
    }
    public boolean accountExists(UUID userId) { return accounts.existsById(userId); }
    public Optional<MonthlyBudget> find(UUID userId, YearMonth month) {
        return budgets.findByUserIdAndMonth(userId, month.toString()).map(MonthlyBudgetEntity::toDomain);
    }
    public MonthlyBudget create(UUID userId, YearMonth month, long amount, Instant now) {
        return budgets.saveAndFlush(new MonthlyBudgetEntity(userId, month, amount, now)).toDomain();
    }
    public MonthlyBudget update(MonthlyBudget current, long amount, Instant now) {
        var entity=budgets.findById(current.id()).orElseThrow(); entity.changeLimit(amount, now);
        return budgets.saveAndFlush(entity).toDomain();
    }
}
