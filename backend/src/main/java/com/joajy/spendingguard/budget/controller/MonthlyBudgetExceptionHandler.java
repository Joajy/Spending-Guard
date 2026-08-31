package com.joajy.spendingguard.budget.controller;

import com.joajy.spendingguard.account.service.exception.UserAccountNotFoundException;
import com.joajy.spendingguard.budget.service.BudgetNotFoundException;
import com.joajy.spendingguard.budget.service.StaleBudgetVersionException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes=MonthlyBudgetController.class)
class MonthlyBudgetExceptionHandler {
    @ExceptionHandler({BudgetNotFoundException.class, UserAccountNotFoundException.class})
    ResponseEntity<ProblemDetail> notFound(RuntimeException e) { return response(HttpStatus.NOT_FOUND,"Budget resource not found",e); }
    @ExceptionHandler({StaleBudgetVersionException.class, OptimisticLockingFailureException.class})
    ResponseEntity<ProblemDetail> conflict(RuntimeException e) { return response(HttpStatus.CONFLICT,"Stale budget version",e); }
    private ResponseEntity<ProblemDetail> response(HttpStatus status,String title,RuntimeException e) {
        var body=ProblemDetail.forStatusAndDetail(status,e.getMessage()); body.setTitle(title);
        return ResponseEntity.status(status).body(body);
    }
}
