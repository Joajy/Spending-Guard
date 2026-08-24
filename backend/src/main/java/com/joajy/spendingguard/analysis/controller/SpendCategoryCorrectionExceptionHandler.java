package com.joajy.spendingguard.analysis.controller;

import com.joajy.spendingguard.analysis.service.exception.SpendCategoryCorrectionNotFoundException;
import com.joajy.spendingguard.analysis.service.exception.SpendCategoryNotEditableException;
import com.joajy.spendingguard.analysis.service.exception.SpendCategoryVersionConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 카테고리 수정 실패를 프론트엔드가 구분할 수 있는 Problem Details 응답으로 변환한다. */
@RestControllerAdvice(assignableTypes = SpendCategoryCorrectionController.class)
class SpendCategoryCorrectionExceptionHandler {

    @ExceptionHandler(SpendCategoryCorrectionNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(SpendCategoryCorrectionNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Spend event not found", exception.getMessage());
    }

    @ExceptionHandler(SpendCategoryNotEditableException.class)
    ResponseEntity<ProblemDetail> notEditable(SpendCategoryNotEditableException exception) {
        return problem(HttpStatus.CONFLICT, "Category is not editable", exception.getMessage());
    }

    @ExceptionHandler(SpendCategoryVersionConflictException.class)
    ResponseEntity<ProblemDetail> versionConflict(SpendCategoryVersionConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Category version conflict", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> invalidRequest(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("요청 값을 확인해 주세요.");
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> unreadableRequest() {
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
