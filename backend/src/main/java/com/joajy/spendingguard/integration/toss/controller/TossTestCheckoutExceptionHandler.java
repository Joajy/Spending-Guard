package com.joajy.spendingguard.integration.toss.controller;

import com.joajy.spendingguard.integration.toss.service.TossPaymentVerificationException;
import com.joajy.spendingguard.integration.toss.service.TossTestOrderStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 테스트 결제의 입력·상태·외부 승인 오류를 HTTP 의미로 변환한다. */
@RestControllerAdvice(assignableTypes = TossTestCheckoutController.class)
class TossTestCheckoutExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> invalidRequest(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, "Invalid Toss test order", exception.getMessage());
    }

    @ExceptionHandler(TossTestOrderStateException.class)
    ResponseEntity<ProblemDetail> invalidState(TossTestOrderStateException exception) {
        return response(HttpStatus.CONFLICT, "Toss test order conflict", exception.getMessage());
    }

    @ExceptionHandler({TossPaymentVerificationException.class, IllegalStateException.class})
    ResponseEntity<ProblemDetail> unavailable(RuntimeException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Toss payment confirmation failed", exception.getMessage());
    }

    private ResponseEntity<ProblemDetail> response(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }
}
