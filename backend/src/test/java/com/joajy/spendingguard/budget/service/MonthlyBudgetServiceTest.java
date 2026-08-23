package com.joajy.spendingguard.budget.service;

import java.time.*;
import java.util.*;
import com.joajy.spendingguard.budget.domain.MonthlyBudget;
import com.joajy.spendingguard.budget.service.port.MonthlyBudgetStore;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

class MonthlyBudgetServiceTest {
    private static final UUID USER=UUID.fromString("9bbd364c-a952-42aa-91cb-f607603aa7d5");
    private static final YearMonth MONTH=YearMonth.of(2026,8);
    private static final Instant NOW=Instant.parse("2026-08-23T01:00:00Z");
    private final MonthlyBudgetStore store=mock(MonthlyBudgetStore.class);
    private final MonthlyBudgetService service=new MonthlyBudgetService(store,Clock.fixed(NOW,ZoneOffset.UTC));

    @Test void createsFirstMonthlyBudget() {
        given(store.accountExists(USER)).willReturn(true); given(store.find(USER,MONTH)).willReturn(Optional.empty());
        var created=budget(500_000,0); given(store.create(USER,MONTH,500_000,NOW)).willReturn(created);
        assertThat(service.set(USER,MONTH,500_000,null)).isEqualTo(created);
    }
    @Test void updatesWithCurrentVersion() {
        var current=budget(500_000,2); given(store.accountExists(USER)).willReturn(true);
        given(store.find(USER,MONTH)).willReturn(Optional.of(current)); given(store.update(current,600_000,NOW)).willReturn(budget(600_000,3));
        assertThat(service.set(USER,MONTH,600_000,2L).limitAmount()).isEqualTo(600_000);
    }
    @Test void rejectsStaleVersion() {
        given(store.accountExists(USER)).willReturn(true); given(store.find(USER,MONTH)).willReturn(Optional.of(budget(500_000,2)));
        assertThatThrownBy(() -> service.set(USER,MONTH,600_000,1L)).isInstanceOf(StaleBudgetVersionException.class);
    }
    @Test void calculatesRemainingAmount() { assertThat(budget(500_000,0).remainingAmount()).isEqualTo(400_000); }
    private MonthlyBudget budget(long limit,long version) { return new MonthlyBudget(UUID.randomUUID(),USER,MONTH,limit,100_000,version,NOW); }
}
