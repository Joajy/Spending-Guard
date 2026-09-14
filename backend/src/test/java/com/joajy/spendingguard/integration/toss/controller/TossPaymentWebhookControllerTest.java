package com.joajy.spendingguard.integration.toss.controller;

import com.joajy.spendingguard.integration.toss.service.TossPaymentWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TossPaymentWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class TossPaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TossPaymentWebhookService service;

    @Test
    void acknowledgesVerifiedPaymentStatusChange() throws Exception {
        given(service.process("PAYMENT_STATUS_CHANGED", "payment-key-1"))
                .willReturn(new TossPaymentWebhookService.Result(false, 2, 0));

        mockMvc.perform(post("/api/v1/integrations/toss-payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "PAYMENT_STATUS_CHANGED",
                                  "data": {"paymentKey": "payment-key-1"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ignored").value(false))
                .andExpect(jsonPath("$.acceptedCount").value(2))
                .andExpect(jsonPath("$.duplicateCount").value(0));
    }

    @Test
    void rejectsMissingPaymentKey() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/toss-payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "PAYMENT_STATUS_CHANGED",
                                  "data": {}
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
