package com.joajy.spendingguard.account.controller;

import java.util.UUID;
import com.joajy.spendingguard.account.service.exception.InvalidVerificationCodeException;
import com.joajy.spendingguard.account.service.exception.VerificationCodeRequestTooFrequentException;
import com.joajy.spendingguard.account.service.port.inbound.VerifyEmailUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmailVerificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmailVerificationControllerTest {
    private static final UUID USER_ID = UUID.fromString("9bbd364c-a952-42aa-91cb-f607603aa7d5");
    @Autowired MockMvc mockMvc;
    @MockitoBean VerifyEmailUseCase useCase;

    @Test void acceptsIssuingVerificationCode() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/email-verification", USER_ID)).andExpect(status().isAccepted());
        then(useCase).should().issue(USER_ID);
    }

    @Test void confirmsSixDigitCode() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/email-verification/confirmation", USER_ID)
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"123456\"}"))
                .andExpect(status().isNoContent());
        then(useCase).should().confirm(USER_ID, "123456");
    }

    @Test void returnsTooManyRequestsForImmediateResend() throws Exception {
        willThrow(new VerificationCodeRequestTooFrequentException()).given(useCase).issue(USER_ID);

        mockMvc.perform(post("/api/v1/users/{id}/email-verification", USER_ID))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Verification temporarily limited"));
    }

    @Test void rejectsMalformedCodeBeforeUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/email-verification/confirmation", USER_ID)
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"123\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test void rejectsMissingCodeBeforeUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/email-verification/confirmation", USER_ID)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test void returnsUnprocessableEntityForWrongOrExpiredCode() throws Exception {
        willThrow(new InvalidVerificationCodeException()).given(useCase).confirm(USER_ID, "999999");
        mockMvc.perform(post("/api/v1/users/{id}/email-verification/confirmation", USER_ID)
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"999999\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid verification code"));
    }
}
