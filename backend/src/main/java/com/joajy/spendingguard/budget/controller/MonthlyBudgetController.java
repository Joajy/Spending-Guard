package com.joajy.spendingguard.budget.controller;

import java.time.YearMonth;
import java.util.UUID;
import com.joajy.spendingguard.budget.service.MonthlyBudgetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}/budgets/{month}")
public class MonthlyBudgetController {
    private final MonthlyBudgetService service;
    MonthlyBudgetController(MonthlyBudgetService service) { this.service=service; }
    @GetMapping ResponseEntity<MonthlyBudgetResponse> get(@PathVariable UUID userId, @PathVariable YearMonth month) {
        return ResponseEntity.ok(MonthlyBudgetResponse.from(service.get(userId, month)));
    }
    @PutMapping ResponseEntity<MonthlyBudgetResponse> set(@PathVariable UUID userId, @PathVariable YearMonth month,
                                                          @Valid @RequestBody SetMonthlyBudgetRequest request) {
        return ResponseEntity.ok(MonthlyBudgetResponse.from(service.set(userId, month, request.amount(), request.version())));
    }
}
