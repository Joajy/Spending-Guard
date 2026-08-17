package com.joajy.spendingguard.spendevent.application.result;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;

/**
 * 소비 이벤트 접수 유스케이스의 처리 결과다.
 * 전송 계층에 종속되지 않은 형태로 새 이벤트의 식별자와 현재 상태, 서버 접수 시각을 전달한다.
 */
public record SpendEventReceipt(
        UUID eventId,
        SpendEventStatus status,
        Instant receivedAt
) {
}
