package com.joajy.spendingguard.spendevent;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitSpendEventRequest(
        @NotNull SpendEventSource source,
        @Size(max = 200) String externalEventId,
        @NotBlank @Size(max = 2000) String message,
        Instant occurredAt
) {
}
