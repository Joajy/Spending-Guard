package com.joajy.spendingguard.account.api.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;

public record ConfirmEmailRequest(
        @NotNull(message = "must be provided")
        @Pattern(regexp = "\\d{6}", message = "must be a 6-digit number") String code
) { }
