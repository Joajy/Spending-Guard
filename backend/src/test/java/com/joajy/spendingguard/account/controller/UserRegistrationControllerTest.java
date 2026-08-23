package com.joajy.spendingguard.account.controller;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.account.service.exception.DuplicateEmailException;
import com.joajy.spendingguard.account.service.port.inbound.RegisterUserUseCase;
import com.joajy.spendingguard.account.service.result.UserRegistration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @Test
    void createsUserWithoutExposingPassword() throws Exception {
        UUID userId = UUID.fromString("9bbd364c-a952-42aa-91cb-f607603aa7d5");
        given(registerUserUseCase.register(any())).willReturn(new UserRegistration(
                userId,
                "user@example.com",
                Instant.parse("2026-08-20T01:00:00Z")
        ));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "safe-password-123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/users/" + userId))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void acceptsEmailWithSurroundingWhitespace() throws Exception {
        UUID userId = UUID.fromString("9bbd364c-a952-42aa-91cb-f607603aa7d5");
        given(registerUserUseCase.register(any())).willReturn(new UserRegistration(
                userId,
                "user@example.com",
                Instant.parse("2026-08-20T01:00:00Z")
        ));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "  User@Example.COM  ",
                                  "password": "safe-password-123"
                                }
                                """))
                .andExpect(status().isCreated());

        then(registerUserUseCase).should().register(argThat(command ->
                command.email().equals("User@Example.COM")
        ));
    }

    @Test
    void rejectsMissingEmail() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "safe-password-123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("email:")));

        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    void rejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("password:")));

        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    void returnsConflictForDuplicateEmail() throws Exception {
        given(registerUserUseCase.register(any())).willThrow(new DuplicateEmailException());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "safe-password-123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate email"));
    }
}
