package com.joajy.spendingguard.spendevent.controller.exception;

import com.joajy.spendingguard.spendevent.controller.SpendEventController;
import com.joajy.spendingguard.spendevent.service.exception.DuplicateSpendEventException;
import com.joajy.spendingguard.spendevent.service.exception.SpendEventNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 소비 이벤트 API에서 발생하는 입력 오류와 중복 접수를 일관된 Problem Details 응답으로 변환한다.
 *
 * <p>중복 알림은 {@code 409 Conflict}, 필드 검증과 읽을 수 없는 JSON은
 * {@code 400 Bad Request}로 구분한다. 오류 본문은 RFC 9457 계열의
 * {@link ProblemDetail} 형식을 사용한다.
 *
 * <p>적용 대상을 {@link SpendEventController}로 제한해 다른 API가 각자의 오류 계약을
 * 독립적으로 정의할 수 있게 한다. 예상하지 못한 서버 오류는 여기서 숨기지 않고 공통
 * 오류 처리와 관측 계층으로 전파한다.
 */
@RestControllerAdvice(assignableTypes = SpendEventController.class)
class SpendEventExceptionHandler {

    @ExceptionHandler(DuplicateSpendEventException.class)
    ResponseEntity<ProblemDetail> handleDuplicate(DuplicateSpendEventException exception) {
        return problem(HttpStatus.CONFLICT, "Duplicate spend event", exception.getMessage());
    }

    @ExceptionHandler(SpendEventNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(SpendEventNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Spend event not found", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleInvalidPath() {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "eventId 형식을 확인해 주세요.");
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

