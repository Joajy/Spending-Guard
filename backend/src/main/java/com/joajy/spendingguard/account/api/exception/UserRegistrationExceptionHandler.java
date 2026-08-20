package com.joajy.spendingguard.account.api.exception;

import com.joajy.spendingguard.account.api.controller.UserRegistrationController;
import com.joajy.spendingguard.account.application.exception.DuplicateEmailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 회원 등록의 입력 오류와 이메일 중복을 일관된 Problem Details 응답으로 변환한다. */
@RestControllerAdvice(assignableTypes = UserRegistrationController.class)
class UserRegistrationExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ProblemDetail> handleDuplicateEmail(DuplicateEmailException exception) {
        return problem(HttpStatus.CONFLICT, "Duplicate email", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("요청 값을 확인해 주세요.");
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableRequest() {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "요청 형식을 확인해 주세요.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
