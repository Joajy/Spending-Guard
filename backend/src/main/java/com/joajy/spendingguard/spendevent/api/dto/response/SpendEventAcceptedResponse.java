package com.joajy.spendingguard.spendevent.api.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.application.result.SpendEventReceipt;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;

/**
 * 소비 이벤트가 정상 접수되었음을 클라이언트에 알리는 HTTP 응답 모델이다.
 * 애플리케이션 결과에서 외부에 필요한 이벤트 ID, 상태, 접수 시각만 골라 반환한다.
 */
public record SpendEventAcceptedResponse(
        UUID eventId,
        SpendEventStatus status,
        Instant receivedAt
) {

    public static SpendEventAcceptedResponse from(SpendEventReceipt receipt) {
        return new SpendEventAcceptedResponse(receipt.eventId(), receipt.status(), receipt.receivedAt());
    }
}
