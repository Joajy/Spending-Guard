package com.joajy.spendingguard.spendevent.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;

/**
 * 소비 알림의 접수가 끝났음을 후속 처리 시스템에 전달하는 도메인 이벤트다.
 * 원문이나 개인정보는 포함하지 않고 이벤트 식별자, 유입 경로, 접수 시각만 전달해 필요한 데이터는 저장소에서 다시 조회하게 한다.
 */
public record SpendEventReceived(
        UUID eventId,
        SpendEventSource source,
        Instant receivedAt
) {
}
