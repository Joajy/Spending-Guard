package com.joajy.spendingguard.spendevent.service.result;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;

/** 원천 이벤트와 선택적인 빠른 파싱 결과를 결합한 조회 유스케이스 결과다. */
public record SpendEventDetail(
        UUID eventId,
        SpendEventSource source,
        SpendEventStatus status,
        Instant occurredAt,
        Instant receivedAt,
        FastParseResult fastParse
) {
}

