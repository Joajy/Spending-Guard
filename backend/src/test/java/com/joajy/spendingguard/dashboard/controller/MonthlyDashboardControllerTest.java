package com.joajy.spendingguard.dashboard.controller;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import com.joajy.spendingguard.dashboard.service.MonthlyDashboardService;
import com.joajy.spendingguard.dashboard.service.result.BudgetSummary;
import com.joajy.spendingguard.dashboard.service.result.CategorySpending;
import com.joajy.spendingguard.dashboard.service.result.MonthlyDashboard;
import com.joajy.spendingguard.dashboard.service.result.RiskCount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonthlyDashboardController.class)
class MonthlyDashboardControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonthlyDashboardService service;

    @Test
    void returnsMonthlyDashboardForFrontend() throws Exception {
        given(service.get(USER_ID, YearMonth.of(2026, 8))).willReturn(new MonthlyDashboard(
                YearMonth.of(2026, 8),
                new BudgetSummary(500_000, 40_800, 459_200),
                40_800,
                2,
                List.of(new CategorySpending("TRANSPORT", 28_000, 1)),
                List.of(new RiskCount("HIGH", 1))
        ));

        mockMvc.perform(get("/api/v1/users/{userId}/dashboard", USER_ID)
                        .queryParam("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-08"))
                .andExpect(jsonPath("$.budget.remainingAmount").value(459_200))
                .andExpect(jsonPath("$.totalSpending").value(40_800))
                .andExpect(jsonPath("$.transactionCount").value(2))
                .andExpect(jsonPath("$.categories[0].category").value("TRANSPORT"))
                .andExpect(jsonPath("$.risks[0].riskLevel").value("HIGH"));
    }

    @Test
    void rejectsInvalidMonthFormat() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/dashboard", USER_ID)
                        .queryParam("month", "2026-8"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid dashboard month"));
    }
}
