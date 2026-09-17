package com.joajy.spendingguard.spendevent.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import com.joajy.spendingguard.spendevent.service.exception.InvalidSpendEventQueryException;
import com.joajy.spendingguard.spendevent.service.port.inbound.GetSpendEventUseCase;
import com.joajy.spendingguard.spendevent.service.port.inbound.ListSpendEventsUseCase;
import com.joajy.spendingguard.spendevent.service.port.inbound.SubmitSpendEventUseCase;
import com.joajy.spendingguard.spendevent.service.result.SpendEventHistoryItem;
import com.joajy.spendingguard.spendevent.service.result.SpendEventHistoryPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpendEventController.class)
@AutoConfigureMockMvc(addFilters = false)
class SpendEventHistoryControllerTest {

    private static final UUID USER_ID = UUID.fromString("3f6d4218-f5a6-48cf-9813-006779108c0d");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmitSpendEventUseCase submitSpendEventUseCase;

    @MockitoBean
    private GetSpendEventUseCase getSpendEventUseCase;

    @MockitoBean
    private ListSpendEventsUseCase listSpendEventsUseCase;

    @Test
    void returnsFilteredTransactionHistory() throws Exception {
        UUID eventId = UUID.fromString("9bbd364c-a952-42aa-91cb-f607603aa7d5");
        given(listSpendEventsUseCase.list(
                USER_ID, YearMonth.of(2026, 8), SpendEventStatus.COMPLETED,
                "SHOPPING", null, 20
        )).willReturn(new SpendEventHistoryPage(
                List.of(new SpendEventHistoryItem(
                        eventId,
                        "쿠팡 **,***원 결제",
                        SpendEventStatus.COMPLETED,
                        Instant.parse("2026-08-18T01:00:00Z"),
                        new BigDecimal("12800"),
                        "PAYMENT",
                        "SHOPPING",
                        false,
                        "LOW",
                        0
                )),
                "next-page",
                true
        ));

        mockMvc.perform(get("/api/v1/users/{userId}/spend-events", USER_ID)
                        .param("month", "2026-08")
                        .param("status", "COMPLETED")
                        .param("category", "SHOPPING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.items[0].displayText").value("쿠팡 **,***원 결제"))
                .andExpect(jsonPath("$.items[0].currency").value("KRW"))
                .andExpect(jsonPath("$.items[0].category").value("SHOPPING"))
                .andExpect(jsonPath("$.items[0].categoryVersion").value(0))
                .andExpect(jsonPath("$.nextCursor").value("next-page"))
                .andExpect(jsonPath("$.hasNext").value(true));

        verify(listSpendEventsUseCase).list(
                USER_ID, YearMonth.of(2026, 8), SpendEventStatus.COMPLETED,
                "SHOPPING", null, 20
        );
    }

    @Test
    void rejectsMalformedMonth() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/spend-events", USER_ID)
                        .param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("month 값을 확인해 주세요."));
    }

    @Test
    void returnsBadRequestForUnsupportedCategory() throws Exception {
        given(listSpendEventsUseCase.list(
                USER_ID, YearMonth.of(2026, 8), null, "ENTERTAINMENT", null, 20
        )).willThrow(new InvalidSpendEventQueryException("category 값을 확인해 주세요."));

        mockMvc.perform(get("/api/v1/users/{userId}/spend-events", USER_ID)
                        .param("month", "2026-08")
                        .param("category", "ENTERTAINMENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid query"))
                .andExpect(jsonPath("$.detail").value("category 값을 확인해 주세요."));
    }
}
