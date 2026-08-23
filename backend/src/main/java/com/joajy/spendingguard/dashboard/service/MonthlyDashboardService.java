package com.joajy.spendingguard.dashboard.service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

import com.joajy.spendingguard.account.service.exception.UserAccountNotFoundException;
import com.joajy.spendingguard.dashboard.service.port.MonthlyDashboardQuery;
import com.joajy.spendingguard.dashboard.service.result.MonthlyDashboard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 한 화면에 필요한 월 예산과 소비 통계를 일관된 조회 모델로 조립한다. */
@Service
public class MonthlyDashboardService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private final MonthlyDashboardQuery query;

    public MonthlyDashboardService(MonthlyDashboardQuery query) {
        this.query = query;
    }

    @Transactional(readOnly = true)
    public MonthlyDashboard get(UUID userId, YearMonth month) {
        if (!query.userExists(userId)) {
            throw new UserAccountNotFoundException();
        }
        var from = month.atDay(1).atStartOfDay(SERVICE_ZONE).toInstant();
        var until = month.plusMonths(1).atDay(1).atStartOfDay(SERVICE_ZONE).toInstant();
        var categories = query.findCategorySpending(userId, from, until);
        long total = categories.stream().mapToLong(category -> category.amount()).sum();
        long count = categories.stream().mapToLong(category -> category.transactionCount()).sum();
        return new MonthlyDashboard(
                month,
                query.findBudget(userId, month).orElse(null),
                total,
                count,
                categories,
                query.findRiskCounts(userId, from, until)
        );
    }
}
