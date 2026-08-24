package com.joajy.spendingguard.auth.controller;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.auth.service.TokenLifecycleService;
import com.joajy.spendingguard.auth.service.exception.InvalidRefreshTokenException;
import com.joajy.spendingguard.auth.service.result.AuthTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TokenLifecycleController.class)
@AutoConfigureMockMvc(addFilters = false)
class TokenLifecycleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenLifecycleService service;

    @Test
    void returnsRotatedTokenPair() throws Exception {
        UUID userId = UUID.randomUUID();
        given(service.refresh("current")).willReturn(new AuthTokens(
                "Bearer", "new-access", Instant.parse("2026-08-24T01:15:00Z"),
                "new-refresh", Instant.parse("2026-09-07T01:00:00Z"), userId
        ));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"current\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void rejectsReusedRefreshToken() throws Exception {
        given(service.refresh("reused")).willThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"reused\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid refresh token"));
    }

    @Test
    void logsOutWithoutRevealingWhetherTokenExisted() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"current\"}"))
                .andExpect(status().isNoContent());

        verify(service).logout("current");
    }
}
