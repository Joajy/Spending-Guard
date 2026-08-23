package com.joajy.spendingguard.budget.controller;

import java.time.*;
import java.util.UUID;
import com.joajy.spendingguard.budget.domain.MonthlyBudget;
import com.joajy.spendingguard.budget.service.MonthlyBudgetService;
import com.joajy.spendingguard.budget.service.StaleBudgetVersionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MonthlyBudgetController.class)
class MonthlyBudgetControllerTest {
    private static final UUID USER=UUID.fromString("9bbd364c-a952-42aa-91cb-f607603aa7d5");
    private static final YearMonth MONTH=YearMonth.of(2026,8);
    @Autowired MockMvc mockMvc;
    @MockitoBean MonthlyBudgetService service;

    @Test void returnsBudgetWithRemainingAmount() throws Exception {
        given(service.get(USER,MONTH)).willReturn(budget(500_000,100_000,0));
        mockMvc.perform(get("/api/v1/users/{userId}/budgets/{month}",USER,"2026-08"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.remainingAmount").value(400_000));
    }
    @Test void createsBudgetWithoutVersion() throws Exception {
        given(service.set(USER,MONTH,500_000,null)).willReturn(budget(500_000,0,0));
        mockMvc.perform(put("/api/v1/users/{userId}/budgets/{month}",USER,"2026-08")
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":500000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0));
    }
    @Test void returnsConflictForStaleVersion() throws Exception {
        given(service.set(USER,MONTH,600_000,0L)).willThrow(new StaleBudgetVersionException());
        mockMvc.perform(put("/api/v1/users/{userId}/budgets/{month}",USER,"2026-08")
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":600000,\"version\":0}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.title").value("Stale budget version"));
    }
    private MonthlyBudget budget(long limit,long spent,long version) {
        return new MonthlyBudget(UUID.randomUUID(),USER,MONTH,limit,spent,version,Instant.parse("2026-08-23T01:00:00Z"));
    }
}
