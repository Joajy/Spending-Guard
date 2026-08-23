package com.joajy.spendingguard.dashboard.controller;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import com.joajy.spendingguard.dashboard.service.MonthlyDashboardService;
import com.joajy.spendingguard.dashboard.service.result.MonthlyDashboard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/dashboard")
public class MonthlyDashboardController {

    private final MonthlyDashboardService service;

    public MonthlyDashboardController(MonthlyDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public MonthlyDashboard get(
            @PathVariable UUID userId,
            @RequestParam String month
    ) {
        try {
            return service.get(userId, YearMonth.parse(month));
        } catch (DateTimeParseException exception) {
            throw new InvalidDashboardMonthException();
        }
    }
}
