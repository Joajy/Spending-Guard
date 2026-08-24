package com.joajy.spendingguard.spendevent.service.port.inbound;

import java.time.YearMonth;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import com.joajy.spendingguard.spendevent.service.result.SpendEventHistoryPage;

/** 인증된 사용자의 월별 소비 내역을 조건에 맞춰 조회하는 입력 포트다. */
public interface ListSpendEventsUseCase {
    SpendEventHistoryPage list(
            UUID userId,
            YearMonth month,
            SpendEventStatus status,
            String category,
            String cursor,
            int size
    );
}
