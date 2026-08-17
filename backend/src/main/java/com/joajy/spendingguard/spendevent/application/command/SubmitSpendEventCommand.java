package com.joajy.spendingguard.spendevent.application.command;

import java.time.Instant;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;

/**
 * 소비 이벤트 접수 유스케이스에 전달되는 입력 명령이다.
 *
 * <p>HTTP 요청 객체와 분리되어 있어 다른 수집 채널도 같은 유스케이스를 재사용한다.
 * 메시지는 아직 정제되지 않은 입력이며, 저장 직전에 도메인 정책을 거친다.
 *
 * @param source 이벤트가 유입된 채널
 * @param externalEventId 채널이 보장하는 이벤트 식별자, 제공되지 않으면 {@code null}
 * @param message 정제 전 소비 알림 텍스트
 * @param occurredAt 외부 채널 기준 발생 시각, 알 수 없으면 {@code null}
 */
public record SubmitSpendEventCommand(
        SpendEventSource source,
        String externalEventId,
        String message,
        Instant occurredAt
) {
}
