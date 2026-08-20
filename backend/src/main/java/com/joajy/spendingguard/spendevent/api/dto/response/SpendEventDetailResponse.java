package com.joajy.spendingguard.spendevent.api.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.application.result.SpendEventDetail;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;

/**
 * 비동기 소비 이벤트의 현재 상태를 프론트엔드에 전달하는 조회 응답이다.
 *
 * <p>접수 직후에는 {@code fastParse}가 {@code null}일 수 있다. 클라이언트는 이를 오류로
 * 해석하지 않고 {@code status}가 바뀔 때까지 같은 리소스를 다시 조회할 수 있다.
 */
public record SpendEventDetailResponse(
        UUID eventId,
        SpendEventSource source,
        SpendEventStatus status,
        Instant occurredAt,
        Instant receivedAt,
        FastParseResultResponse fastParse
) {

    public static SpendEventDetailResponse from(SpendEventDetail detail) {
        return new SpendEventDetailResponse(
                detail.eventId(),
                detail.source(),
                detail.status(),
                detail.occurredAt(),
                detail.receivedAt(),
                FastParseResultResponse.from(detail.fastParse())
        );
    }
}

