package com.joajy.spendingguard.spendevent.api;

import java.time.Instant;

import com.joajy.spendingguard.spendevent.application.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.domain.SpendEventSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitSpendEventRequest(
        @NotNull SpendEventSource source,
        @Size(max = 200) String externalEventId,
        @NotBlank @Size(max = 2000) String message,
        Instant occurredAt
) {

    SubmitSpendEventCommand toCommand() {
        return new SubmitSpendEventCommand(source, externalEventId, message, occurredAt);
    }
}
