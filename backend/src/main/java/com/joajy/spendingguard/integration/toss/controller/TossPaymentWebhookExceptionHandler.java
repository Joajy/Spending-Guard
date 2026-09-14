package com.joajy.spendingguard.integration.toss.controller;

import com.joajy.spendingguard.integration.toss.service.TossPaymentVerificationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Toss 웹훅 검증 실패를 재시도 가능한 HTTP 오류로 변환한다. */
@RestControllerAdvice(assignableTypes = TossPaymentWebhookController.class)
class TossPaymentWebhookExceptionHandler {

    @ExceptionHandler({TossPaymentVerificationException.class, IllegalStateException.class})
    ResponseEntity<ProblemDetail> handleVerificationFailure(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage()
        );
        problem.setTitle("Toss payment verification failed");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}
