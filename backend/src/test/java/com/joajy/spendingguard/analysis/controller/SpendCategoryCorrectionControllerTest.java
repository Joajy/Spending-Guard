package com.joajy.spendingguard.analysis.controller;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.analysis.domain.model.SpendCategory;
import com.joajy.spendingguard.analysis.service.exception.SpendCategoryNotEditableException;
import com.joajy.spendingguard.analysis.service.exception.SpendCategoryVersionConflictException;
import com.joajy.spendingguard.analysis.service.port.inbound.CorrectSpendCategoryUseCase;
import com.joajy.spendingguard.analysis.service.result.SpendCategoryCorrectionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpendCategoryCorrectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SpendCategoryCorrectionControllerTest {

    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID EVENT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CorrectSpendCategoryUseCase useCase;

    @Test
    void correctsCategoryAndReturnsOriginalClassification() throws Exception {
        given(useCase.correct(any())).willReturn(new SpendCategoryCorrectionResult(
                EVENT_ID,
                SpendCategory.SHOPPING,
                SpendCategory.SUBSCRIPTION,
                true,
                RiskLevel.LOW,
                "NORMAL_PATTERN",
                1,
                Instant.parse("2026-08-25T01:00:00Z")
        ));

        mockMvc.perform(patch(
                        "/api/v1/users/{userId}/spend-events/{eventId}/category",
                        USER_ID, EVENT_ID
                ).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "category": "SUBSCRIPTION",
                          "expectedVersion": 0
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.originalCategory").value("SHOPPING"))
                .andExpect(jsonPath("$.category").value("SUBSCRIPTION"))
                .andExpect(jsonPath("$.fixedCost").value(true))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void returnsConflictForStaleVersion() throws Exception {
        given(useCase.correct(any())).willThrow(new SpendCategoryVersionConflictException());

        mockMvc.perform(patch(
                        "/api/v1/users/{userId}/spend-events/{eventId}/category",
                        USER_ID, EVENT_ID
                ).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"category": "OTHER", "expectedVersion": 0}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Category version conflict"));
    }

    @Test
    void returnsConflictWhileAnalysisIsIncomplete() throws Exception {
        given(useCase.correct(any())).willThrow(new SpendCategoryNotEditableException());

        mockMvc.perform(patch(
                        "/api/v1/users/{userId}/spend-events/{eventId}/category",
                        USER_ID, EVENT_ID
                ).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"category": "OTHER", "expectedVersion": 0}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Category is not editable"));
    }

    @Test
    void rejectsUnsupportedCategory() throws Exception {
        mockMvc.perform(patch(
                        "/api/v1/users/{userId}/spend-events/{eventId}/category",
                        USER_ID, EVENT_ID
                ).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"category": "ENTERTAINMENT", "expectedVersion": 0}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }
}
