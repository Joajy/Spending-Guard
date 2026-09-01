package com.joajy.spendingguard.auth.security;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import com.joajy.spendingguard.auth.config.SecurityConfiguration;
import com.joajy.spendingguard.dashboard.controller.MonthlyDashboardController;
import com.joajy.spendingguard.dashboard.service.MonthlyDashboardService;
import com.joajy.spendingguard.dashboard.service.result.MonthlyDashboard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonthlyDashboardController.class)
@Import({SecurityConfiguration.class, SecurityProblemWriter.class, UserScopeAuthorization.class})
class ProtectedUserApiSecurityTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonthlyDashboardService service;

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/dashboard", USER_ID)
                        .queryParam("month", "2026-08"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication required"));
    }

    @Test
    void allowsDeploymentProbesWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/livez"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/readyz"))
                .andExpect(status().isNotFound());
    }

    @Test
    void allowsTokenOwner() throws Exception {
        given(service.get(USER_ID, YearMonth.of(2026, 8))).willReturn(new MonthlyDashboard(
                YearMonth.of(2026, 8), null, 0, 0, List.of(), List.of()
        ));

        mockMvc.perform(get("/api/v1/users/{userId}/dashboard", USER_ID)
                        .queryParam("month", "2026-08")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAccessToAnotherUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/dashboard", USER_ID)
                        .queryParam("month", "2026-08")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));
    }
}
