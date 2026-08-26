package com.joajy.spendingguard.riskalert.controller;

import com.joajy.spendingguard.account.service.exception.UserAccountNotFoundException;
import com.joajy.spendingguard.riskalert.service.exception.InvalidRiskAlertQueryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = RiskAlertController.class)
class RiskAlertExceptionHandler {

    @ExceptionHandler(UserAccountNotFoundException.class)
    ResponseEntity<ProblemDetail> userNotFound(UserAccountNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "User not found", exception.getMessage());
    }

    @ExceptionHandler(InvalidRiskAlertQueryException.class)
    ResponseEntity<ProblemDetail> invalidQuery(InvalidRiskAlertQueryException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid risk alert query", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> invalidPath(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "요청 값을 확인해 주세요.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
