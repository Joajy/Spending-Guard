package com.joajy.spendingguard.auth.security;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 인증·인가 실패를 다른 API와 같은 RFC 9457 형식으로 반환한다. */
@Component
public class SecurityProblemWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    SecurityProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "Access denied");
    }

    private void write(HttpServletResponse response, HttpStatus status, String title)
            throws IOException {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, title);
        body.setTitle(title);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
