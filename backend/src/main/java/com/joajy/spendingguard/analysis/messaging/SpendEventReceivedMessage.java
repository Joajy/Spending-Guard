package com.joajy.spendingguard.analysis.messaging;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;

/**
 * {@code spend-event.received.v1} 토픽의 버전 1 메시지 계약이다.
 *
 * @param schemaVersion 메시지 스키마 버전, 현재는 1만 지원
 * @param eventId 원천 소비 이벤트 식별자
 * @param source 이벤트 유입 채널
 * @param receivedAt API가 이벤트 접수를 완료한 시각
 */
record SpendEventReceivedMessage(
        int schemaVersion,
        UUID eventId,
        SpendEventSource source,
        Instant receivedAt
) {

    SpendEventReceivedMessage {
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("지원하지 않는 소비 이벤트 스키마입니다.");
        }
        Objects.requireNonNull(eventId, "소비 이벤트 식별자가 필요합니다.");
        Objects.requireNonNull(source, "소비 이벤트 유입 채널이 필요합니다.");
        Objects.requireNonNull(receivedAt, "소비 이벤트 접수 시각이 필요합니다.");
    }
}
