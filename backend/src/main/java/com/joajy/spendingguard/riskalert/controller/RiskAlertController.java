package com.joajy.spendingguard.riskalert.controller;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.riskalert.controller.dto.RiskAlertPageResponse;
import com.joajy.spendingguard.riskalert.service.RiskAlertService;
import com.joajy.spendingguard.riskalert.service.exception.InvalidRiskAlertQueryException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 사용자가 확인해야 할 중간·높음 위험 소비를 월별 피드로 제공한다. */
@RestController
@RequestMapping("/api/v1/users/{userId}/risk-alerts")
public class RiskAlertController {

    private final RiskAlertService service;

    public RiskAlertController(RiskAlertService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@userScope.matches(authentication, #userId)")
    public RiskAlertPageResponse list(
            @PathVariable UUID userId,
            @RequestParam String month,
            @RequestParam(defaultValue = "MEDIUM") String minimumLevel,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            return RiskAlertPageResponse.from(service.list(
                    userId,
                    YearMonth.parse(month),
                    RiskLevel.valueOf(minimumLevel.strip().toUpperCase(Locale.ROOT)),
                    cursor,
                    size
            ));
        } catch (DateTimeParseException exception) {
            throw new InvalidRiskAlertQueryException("month는 YYYY-MM 형식이어야 합니다.");
        } catch (IllegalArgumentException exception) {
            throw new InvalidRiskAlertQueryException(
                    "minimumLevel은 MEDIUM 또는 HIGH만 사용할 수 있습니다."
            );
        }
    }
}
