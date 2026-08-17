package com.joajy.spendingguard.outbox.infrastructure.config;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spending-guard.outbox.publisher")
public record OutboxPublisherProperties(
        @NotBlank String topic,
        @Min(1) int batchSize,
        @NotNull Duration leaseDuration,
        @NotNull Duration retryBaseDelay,
        @NotNull Duration retryMaxDelay,
        @NotNull Duration sendTimeout
) {

    public OutboxPublisherProperties {
        requirePositive(leaseDuration, "leaseDuration");
        requirePositive(retryBaseDelay, "retryBaseDelay");
        requirePositive(retryMaxDelay, "retryMaxDelay");
        requirePositive(sendTimeout, "sendTimeout");
    }

    private static void requirePositive(Duration duration, String name) {
        if (duration != null && (duration.isZero() || duration.isNegative())) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}

