package com.joajy.spendingguard.riskalert.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.riskalert.service.RiskAlertService;
import com.joajy.spendingguard.riskalert.service.result.RiskAlertItem;
import com.joajy.spendingguard.riskalert.service.result.RiskAlertPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiskAlertController.class)
@AutoConfigureMockMvc(addFilters = false)
class RiskAlertControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiskAlertService service;

    @Test
    void returnsRiskAlertFeedForFrontend() throws Exception {
        var eventId = UUID.randomUUID();
        given(service.list(eq(USER_ID), any(), eq(RiskLevel.MEDIUM), eq(null), eq(20)))
                .willReturn(new RiskAlertPage(List.of(new RiskAlertItem(
                        eventId,
                        "새벽 2시 카카오T 28,000원 결제",
                        Instant.parse("2026-08-12T17:00:00Z"),
                        BigDecimal.valueOf(28_000),
                        "TRANSPORT",
                        RiskLevel.HIGH,
                        "LATE_NIGHT_TRANSPORT",
                        1
                )), null, false));

        mockMvc.perform(get("/api/v1/users/{userId}/risk-alerts", USER_ID)
                        .queryParam("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.items[0].amount").value(28_000))
                .andExpect(jsonPath("$.items[0].riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.items[0].reasonCode").value("LATE_NIGHT_TRANSPORT"))
                .andExpect(jsonPath("$.items[0].categoryVersion").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void rejectsInvalidMonthAndRiskLevel() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/risk-alerts", USER_ID)
                        .queryParam("month", "2026-8"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid risk alert query"));

        mockMvc.perform(get("/api/v1/users/{userId}/risk-alerts", USER_ID)
                        .queryParam("month", "2026-08")
                        .queryParam("minimumLevel", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid risk alert query"));
    }
}
