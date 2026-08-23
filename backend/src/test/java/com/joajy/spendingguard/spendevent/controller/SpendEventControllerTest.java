package com.joajy.spendingguard.spendevent.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.service.exception.DuplicateSpendEventException;
import com.joajy.spendingguard.spendevent.service.exception.SpendEventNotFoundException;
import com.joajy.spendingguard.spendevent.service.port.inbound.GetSpendEventUseCase;
import com.joajy.spendingguard.spendevent.service.port.inbound.SubmitSpendEventUseCase;
import com.joajy.spendingguard.spendevent.service.result.FastParseResult;
import com.joajy.spendingguard.spendevent.service.result.SpendEventDetail;
import com.joajy.spendingguard.spendevent.service.result.SpendEventReceipt;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpendEventController.class)
class SpendEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmitSpendEventUseCase submitSpendEventUseCase;

    @MockitoBean
    private GetSpendEventUseCase getSpendEventUseCase;

    @Test
    void acceptsValidSpendEvent() throws Exception {
        UUID eventId = UUID.fromString("9bbd364c-a952-42aa-91cb-f607603aa7d5");
        Instant receivedAt = Instant.parse("2026-08-13T01:30:00Z");
        given(submitSpendEventUseCase.submit(any())).willReturn(
                new SpendEventReceipt(eventId, SpendEventStatus.RECEIVED, receivedAt)
        );

        mockMvc.perform(post("/api/v1/spend-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "MANUAL_TEXT",
                                  "message": "테스트상점 12,800원 결제",
                                  "occurredAt": "2026-08-13T01:00:00Z"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "http://localhost/api/v1/spend-events/" + eventId))
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.receivedAt").value("2026-08-13T01:30:00Z"));
    }

    @Test
    void rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/spend-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "MANUAL_TEXT",
                                  "message": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("message:")));

        verifyNoInteractions(submitSpendEventUseCase);
    }

    @Test
    void returnsConflictForDuplicateEvent() throws Exception {
        given(submitSpendEventUseCase.submit(any())).willThrow(new DuplicateSpendEventException());

        mockMvc.perform(post("/api/v1/spend-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "SIMULATOR",
                                  "externalEventId": "event-100",
                                  "message": "테스트상점 12,800원 결제"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate spend event"));
    }

    @Test
    void returnsCurrentStatusAndFastParseResult() throws Exception {
        UUID eventId = UUID.fromString("9bbd364c-a952-42aa-91cb-f607603aa7d5");
        given(getSpendEventUseCase.get(eventId)).willReturn(new SpendEventDetail(
                eventId,
                SpendEventSource.SIMULATOR,
                SpendEventStatus.ANALYZING,
                Instant.parse("2026-08-13T01:00:00Z"),
                Instant.parse("2026-08-13T01:30:00Z"),
                new FastParseResult(
                        new BigDecimal("12800.00"),
                        "PAYMENT",
                        "PARSED",
                        null,
                        "fast-parser-v1",
                        Instant.parse("2026-08-13T01:31:00Z")
                )
        ));

        mockMvc.perform(get("/api/v1/spend-events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.source").value("SIMULATOR"))
                .andExpect(jsonPath("$.status").value("ANALYZING"))
                .andExpect(jsonPath("$.fastParse.amount").value(12800))
                .andExpect(jsonPath("$.fastParse.currency").value("KRW"))
                .andExpect(jsonPath("$.fastParse.transactionType").value("PAYMENT"))
                .andExpect(jsonPath("$.fastParse.status").value("PARSED"));
    }

    @Test
    void returnsNullFastParseWhileEventIsWaiting() throws Exception {
        UUID eventId = UUID.fromString("80faad82-d492-45f8-8c6b-86069005d24d");
        given(getSpendEventUseCase.get(eventId)).willReturn(new SpendEventDetail(
                eventId,
                SpendEventSource.MANUAL_TEXT,
                SpendEventStatus.RECEIVED,
                null,
                Instant.parse("2026-08-13T01:30:00Z"),
                null
        ));

        mockMvc.perform(get("/api/v1/spend-events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.fastParse").doesNotExist());
    }

    @Test
    void returnsNotFoundForUnknownEvent() throws Exception {
        UUID eventId = UUID.fromString("b894a4c7-0c4b-453b-8c82-92bbcd6bd8eb");
        given(getSpendEventUseCase.get(eventId)).willThrow(new SpendEventNotFoundException(eventId));

        mockMvc.perform(get("/api/v1/spend-events/{eventId}", eventId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Spend event not found"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(eventId.toString())));
    }

    @Test
    void returnsBadRequestForMalformedEventId() throws Exception {
        mockMvc.perform(get("/api/v1/spend-events/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("eventId 형식을 확인해 주세요."));
    }
}

