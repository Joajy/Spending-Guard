package com.joajy.spendingguard.budget.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MonthlyBudgetJpaRepository extends JpaRepository<MonthlyBudgetEntity, UUID> {
    Optional<MonthlyBudgetEntity> findByUserIdAndMonth(UUID userId, String month);
}
