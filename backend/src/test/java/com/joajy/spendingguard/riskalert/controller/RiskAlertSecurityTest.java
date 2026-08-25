package com.joajy.spendingguard.riskalert.controller;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import com.joajy.spendingguard.auth.config.SecurityConfiguration;
import com.joajy.spendingguard.auth.security.SecurityProblemWriter;
import com.joajy.spendingguard.auth.security.UserScopeAuthorization;
import com.joajy.spendingguard.riskalert.service.RiskAlertService;
import com.joajy.spendingguard.riskalert.service.result.RiskAlertPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiskAlertController.class)
@Import({SecurityConfiguration.class, SecurityProblemWriter.class, UserScopeAuthorization.class})
class RiskAlertSecurityTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiskAlertService service;

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(request())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication required"));
    }

    @Test
    void allowsTokenOwner() throws Exception {
        given(service.list(eq(USER_ID), eq(YearMonth.of(2026, 8)), any(), eq(null), eq(20)))
                .willReturn(new RiskAlertPage(List.of(), null, false));

        mockMvc.perform(request().with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAccessToAnotherUser() throws Exception {
        mockMvc.perform(request().with(jwt().jwt(
                        token -> token.subject(UUID.randomUUID().toString())
                )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request() {
        return get("/api/v1/users/{userId}/risk-alerts", USER_ID)
                .queryParam("month", "2026-08");
    }
}
