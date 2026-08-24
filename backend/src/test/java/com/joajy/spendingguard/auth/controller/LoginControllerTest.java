package com.joajy.spendingguard.auth.controller;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.auth.service.LoginService;
import com.joajy.spendingguard.auth.service.exception.InvalidCredentialsException;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginService service;

    @Test
    void returnsAccessToken() throws Exception {
        UUID userId = UUID.randomUUID();
        given(service.login("user@example.com", "password-123")).willReturn(new AuthTokens(
                "Bearer", "signed-token", Instant.parse("2026-08-23T01:15:00Z"),
                "refresh-token", Instant.parse("2026-09-06T01:00:00Z"), userId
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("signed-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void hidesWhetherEmailOrPasswordWasWrong() throws Exception {
        given(service.login("user@example.com", "wrong"))
                .willThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid credentials"));
    }

    @Test
    void trimsEmailBeforeLogin() throws Exception {
        UUID userId = UUID.randomUUID();
        given(service.login("user@example.com", "password-123")).willReturn(new AuthTokens(
                "Bearer", "signed-token", Instant.parse("2026-08-23T01:15:00Z"),
                "refresh-token", Instant.parse("2026-09-06T01:00:00Z"), userId
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  user@example.com  \",\"password\":\"password-123\"}"))
                .andExpect(status().isOk());

        verify(service).login("user@example.com", "password-123");
    }

    @Test
    void rejectsPasswordLongerThanBcryptLimit() throws Exception {
        String tooLongPassword = "a".repeat(73);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\""
                                + tooLongPassword + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid login request"));

        verifyNoInteractions(service);
    }
}
