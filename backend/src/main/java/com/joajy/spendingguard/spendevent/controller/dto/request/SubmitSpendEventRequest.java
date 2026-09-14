package com.joajy.spendingguard.spendevent.controller.dto.request;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.service.command.SubmitSpendEventCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 소비 이벤트 접수 API의 요청 형식과 입력 제약을 정의한다. */
public record SubmitSpendEventRequest(
        @NotNull SpendEventSource source,
        @Size(max = 200) String externalEventId,
        @NotBlank @Size(max = 2000) String message,
        Instant occurredAt
) {

    /** 연동 전용 출처를 인증된 일반 입력 API가 사칭하지 못하게 한다. */
    @AssertTrue(message = "연동 전용 소비 이벤트 출처는 직접 제출할 수 없습니다.")
    public boolean isClientSubmittableSource() {
        return source != SpendEventSource.TOSS_WEBHOOK;
    }

    public SubmitSpendEventCommand toCommand(UUID userId) {
        return new SubmitSpendEventCommand(userId, source, externalEventId, message, occurredAt);
    }
}
