package com.joajy.spendingguard.analysis.service.port.outbound;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/** 분석이 끝난 결제 금액을 해당 사용자의 월 예산에 한 번만 반영한다. */
public interface ApplyBudgetConsumptionPort {

    boolean apply(UUID userId, UUID eventId, YearMonth month, long amount, Instant appliedAt);
}
