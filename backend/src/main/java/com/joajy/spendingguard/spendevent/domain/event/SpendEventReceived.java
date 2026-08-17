package com.joajy.spendingguard.spendevent.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;

/**
 * 소비 알림의 접수가 끝났음을 후속 처리 시스템에 전달하는 도메인 이벤트다.
 *
 * <p>원문이나 개인정보는 포함하지 않는다. 후속 소비자는 {@code eventId}로 정제된
 * 원천 데이터를 조회하므로 메시지 브로커에 금융 원문이 복제되지 않는다.
 *
 * @param eventId 저장된 원천 소비 이벤트 식별자
 * @param source 이벤트가 유입된 채널
 * @param receivedAt 서버가 접수를 완료한 시각
 */
public record SpendEventReceived(
        UUID eventId,
        SpendEventSource source,
        Instant receivedAt
) {
}
