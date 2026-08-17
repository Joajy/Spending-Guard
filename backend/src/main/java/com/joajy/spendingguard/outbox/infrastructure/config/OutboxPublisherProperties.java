package com.joajy.spendingguard.outbox.infrastructure.config;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 설정 파일의 Outbox 발행 옵션을 타입이 있는 값으로 바인딩한다.
 *
 * <p>애플리케이션 시작 시 필수값과 모든 시간값의 양수 조건을 검증한다. 이 객체는
 * 외부 설정 표현이며, 애플리케이션에서 사용하는 정책 객체는 별도 구성 클래스가 만든다.
 *
 * @param topic 소비 이벤트를 발행할 Kafka 토픽 이름
 * @param batchSize 한 번에 선점할 이벤트의 최대 건수
 * @param leaseDuration 처리 권한이 유효한 시간
 * @param retryBaseDelay 첫 실패 이후의 기본 재시도 지연
 * @param retryMaxDelay 지수 백오프가 넘지 않는 최대 지연
 * @param sendTimeout Kafka 발행 확인을 기다리는 최대 시간
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

    /**
     * 바인딩된 시간 설정이 모두 양수인지 추가 검증한다.
     *
     * @throws IllegalArgumentException 임대, 재시도 또는 전송 제한 시간이 양수가 아닌 경우
     */
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
