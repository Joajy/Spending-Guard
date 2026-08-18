package com.joajy.spendingguard.analysis.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** Kafka 소비 이벤트 Consumer의 토픽, 그룹, 재시도와 DLQ 정책이다. */
@ConfigurationProperties("spending-guard.analysis.consumer")
public record SpendEventConsumerProperties(
        boolean enabled,
        String topic,
        String groupId,
        int maxRetries,
        Duration retryBackoff,
        String deadLetterTopic
) {

    public SpendEventConsumerProperties {
        if (!StringUtils.hasText(topic)) {
            throw new IllegalArgumentException("소비 이벤트 토픽이 필요합니다.");
        }
        if (!StringUtils.hasText(groupId)) {
            throw new IllegalArgumentException("소비 이벤트 Consumer 그룹이 필요합니다.");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Kafka 재시도 횟수는 0 이상이어야 합니다.");
        }
        if (retryBackoff == null || retryBackoff.isNegative()) {
            throw new IllegalArgumentException("Kafka 재시도 간격은 0 이상이어야 합니다.");
        }
        if (!StringUtils.hasText(deadLetterTopic)) {
            throw new IllegalArgumentException("소비 이벤트 DLQ 토픽이 필요합니다.");
        }
    }
}
