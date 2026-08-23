package com.joajy.spendingguard.account.controller.exception;

import com.joajy.spendingguard.account.controller.EmailVerificationController;
import com.joajy.spendingguard.account.service.exception.InvalidVerificationCodeException;
import com.joajy.spendingguard.account.service.exception.TooManyVerificationAttemptsException;
import com.joajy.spendingguard.account.service.exception.UserAccountNotFoundException;
import com.joajy.spendingguard.account.service.exception.VerificationCodeRequestTooFrequentException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = EmailVerificationController.class)
class EmailVerificationExceptionHandler {
    @ExceptionHandler(UserAccountNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(UserAccountNotFoundException e) { return problem(HttpStatus.NOT_FOUND, "User not found", e.getMessage()); }
    @ExceptionHandler(InvalidVerificationCodeException.class)
    ResponseEntity<ProblemDetail> invalid(InvalidVerificationCodeException e) { return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid verification code", e.getMessage()); }
    @ExceptionHandler({VerificationCodeRequestTooFrequentException.class, TooManyVerificationAttemptsException.class})
    ResponseEntity<ProblemDetail> tooManyRequests(RuntimeException e) { return problem(HttpStatus.TOO_MANY_REQUESTS, "Verification temporarily limited", e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e) { return problem(HttpStatus.BAD_REQUEST, "Invalid request", "code must be a 6-digit number"); }
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        var body = ProblemDetail.forStatusAndDetail(status, detail); body.setTitle(title); return ResponseEntity.status(status).body(body);
    }
}
