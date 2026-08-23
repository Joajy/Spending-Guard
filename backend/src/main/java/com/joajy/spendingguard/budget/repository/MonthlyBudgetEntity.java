package com.joajy.spendingguard.budget.repository;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;
import com.joajy.spendingguard.budget.domain.MonthlyBudget;
import jakarta.persistence.*;

@Entity
@Table(name = "monthly_budget", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "budget_month"}))
class MonthlyBudgetEntity {
    @Id private UUID id;
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="budget_month", nullable=false, length=7) private String month;
    @Column(name="limit_amount", nullable=false) private long limitAmount;
    @Column(name="spent_amount", nullable=false) private long spentAmount;
    @Version private long version;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected MonthlyBudgetEntity() { }
    MonthlyBudgetEntity(UUID userId, YearMonth month, long amount, Instant now) {
        this.id=UUID.randomUUID(); this.userId=userId; this.month=month.toString();
        this.limitAmount=amount; this.spentAmount=0; this.updatedAt=now;
    }
    void changeLimit(long amount, Instant now) { this.limitAmount=amount; this.updatedAt=now; }
    MonthlyBudget toDomain() { return new MonthlyBudget(id,userId,YearMonth.parse(month),limitAmount,spentAmount,version,updatedAt); }
}
