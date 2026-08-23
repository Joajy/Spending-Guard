package com.joajy.spendingguard.spendevent.service.result;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;

/**
 * 소비 이벤트 접수 유스케이스의 처리 결과다.
 *
 * <p>전송 계층에 종속되지 않은 형태이며, 저장과 Outbox 기록이 모두 커밋될 수 있는
 * 상태에 도달한 뒤 반환한다.
 *
 * @param eventId 새로 생성된 소비 이벤트 식별자
 * @param status 접수 직후의 도메인 상태
 * @param receivedAt 서버 기준 접수 시각
 */
public record SpendEventReceipt(
        UUID eventId,
        SpendEventStatus status,
        Instant receivedAt
) {
}
