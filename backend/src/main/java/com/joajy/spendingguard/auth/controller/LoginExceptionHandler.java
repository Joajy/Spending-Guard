package com.joajy.spendingguard.auth.controller;

import com.joajy.spendingguard.auth.service.exception.InvalidCredentialsException;
import com.joajy.spendingguard.auth.service.exception.UnverifiedEmailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = LoginController.class)
class LoginExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ProblemDetail> invalidCredentials(InvalidCredentialsException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", exception.getMessage());
    }

    @ExceptionHandler(UnverifiedEmailException.class)
    ResponseEntity<ProblemDetail> unverifiedEmail(UnverifiedEmailException exception) {
        return problem(HttpStatus.FORBIDDEN, "Email verification required", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> invalidRequest() {
        return problem(HttpStatus.BAD_REQUEST, "Invalid login request", "Valid email and password are required");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        return ResponseEntity.status(status).body(body);
    }
}
