package com.joajy.spendingguard.spendevent.api.dto.request;

import java.time.Instant;

import com.joajy.spendingguard.spendevent.application.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
