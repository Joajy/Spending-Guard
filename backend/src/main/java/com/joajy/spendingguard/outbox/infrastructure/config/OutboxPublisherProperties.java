package com.joajy.spendingguard.outbox.infrastructure.config;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 설정 파일의 Outbox 발행 옵션을 타입이 있는 값으로 바인딩한다.
 * 애플리케이션 시작 시 필수값과 양수 시간 조건을 검증해 잘못된 배치·재시도 설정으로 서비스가 실행되지 않게 한다.
 */
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
