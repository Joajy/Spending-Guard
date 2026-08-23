package com.joajy.spendingguard.dashboard.controller;

import com.joajy.spendingguard.account.service.exception.UserAccountNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = MonthlyDashboardController.class)
class MonthlyDashboardExceptionHandler {

    @ExceptionHandler(UserAccountNotFoundException.class)
    ResponseEntity<ProblemDetail> userNotFound(UserAccountNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "User not found", exception.getMessage());
    }

    @ExceptionHandler(InvalidDashboardMonthException.class)
    ResponseEntity<ProblemDetail> invalidMonth(InvalidDashboardMonthException exception) {
        return response(HttpStatus.BAD_REQUEST, "Invalid dashboard month", exception.getMessage());
    }

    private ResponseEntity<ProblemDetail> response(HttpStatus status, String title, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        return ResponseEntity.status(status).body(body);
    }
}
