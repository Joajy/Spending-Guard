package com.joajy.spendingguard.spendevent.api.dto.request;

import java.time.Instant;

import com.joajy.spendingguard.spendevent.application.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 소비 이벤트 접수 API의 요청 형식과 입력 제약을 정의한다.
 * HTTP 검증이 끝난 값만 애플리케이션 명령으로 변환해 웹 계층의 표현이 유스케이스 안으로 퍼지지 않게 한다.
 */
public record SubmitSpendEventRequest(
        @NotNull SpendEventSource source,
        @Size(max = 200) String externalEventId,
        @NotBlank @Size(max = 2000) String message,
        Instant occurredAt
) {

    public SubmitSpendEventCommand toCommand() {
        return new SubmitSpendEventCommand(source, externalEventId, message, occurredAt);
    }
}
