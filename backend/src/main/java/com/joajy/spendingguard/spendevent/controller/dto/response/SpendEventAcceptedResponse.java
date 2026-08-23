package com.joajy.spendingguard.spendevent.controller.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.service.result.SpendEventReceipt;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;

/**
 * 소비 이벤트가 정상 접수되었음을 클라이언트에 알리는 HTTP 응답 모델이다.
 *
 * <p>비동기 분석 완료가 아니라 안전한 저장이 끝났다는 의미다. 애플리케이션 결과에서
 * 후속 조회에 필요한 이벤트 ID, 현재 상태, 서버 접수 시각만 외부에 노출한다.
 *
 * @param eventId 접수된 소비 이벤트 식별자
 * @param status 응답 시점의 처리 상태
 * @param receivedAt 서버가 이벤트를 접수한 UTC 시각
 */
public record SpendEventAcceptedResponse(
        UUID eventId,
        SpendEventStatus status,
        Instant receivedAt
) {

    /**
     * 애플리케이션 접수 결과를 외부 응답 모델로 변환한다.
     *
     * @param receipt 접수 트랜잭션이 반환한 결과
     * @return 외부에 노출할 필드만 포함한 응답
     */
    public static SpendEventAcceptedResponse from(SpendEventReceipt receipt) {
        return new SpendEventAcceptedResponse(receipt.eventId(), receipt.status(), receipt.receivedAt());
    }
}
