package com.joajy.spendingguard.spendevent.application.command;

import java.time.Instant;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;

/**
 * 소비 이벤트 접수 유스케이스에 전달되는 입력 명령이다.
 * HTTP 요청 객체와 분리되어 있어 다른 수집 채널도 같은 애플리케이션 기능을 재사용할 수 있다.
 */
public record SubmitSpendEventCommand(
        SpendEventSource source,
        String externalEventId,
        String message,
        Instant occurredAt
) {
}
