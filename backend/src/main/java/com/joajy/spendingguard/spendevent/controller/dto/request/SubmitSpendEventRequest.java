package com.joajy.spendingguard.spendevent.controller.dto.request;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.service.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 소비 이벤트 접수 API의 요청 형식과 입력 제약을 정의한다.
 *
 * <p>HTTP 검증이 끝난 값만 애플리케이션 명령으로 변환한다. {@code occurredAt}이
 * 없으면 금융 알림이 실제로 발생한 시각을 알 수 없는 입력으로 허용한다.
 *
 * @param source 이벤트가 들어온 수집 채널
 * @param externalEventId 채널이 제공한 이벤트 식별자, 없으면 메시지 기반 중복 키를 사용
 * @param message 분석할 소비 알림 텍스트
 * @param occurredAt 외부 채널이 알려준 결제 발생 시각, 알 수 없으면 {@code null}
 */
public record SubmitSpendEventRequest(
        @NotNull SpendEventSource source,
        @Size(max = 200) String externalEventId,
        @NotBlank @Size(max = 2000) String message,
        Instant occurredAt
) {

    /**
     * 검증된 HTTP 입력을 전송 계층과 무관한 애플리케이션 명령으로 변환한다.
     *
     * @return 같은 입력값을 담은 접수 명령
     */
    public SubmitSpendEventCommand toCommand(UUID userId) {
        return new SubmitSpendEventCommand(userId, source, externalEventId, message, occurredAt);
    }
}
