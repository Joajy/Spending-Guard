package com.joajy.spendingguard.outbox.infrastructure.config;

import com.joajy.spendingguard.outbox.application.service.OutboxPublishPolicy;
import com.joajy.spendingguard.outbox.domain.policy.ExponentialRetryBackoff;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Outbox 발행에 필요한 정책 객체와 Kafka 토픽을 구성하고 스케줄링을 활성화한다.
 * 외부 설정값을 애플리케이션 정책으로 조립하며, 발행 기능을 끈 환경에서는 토픽 생성도 함께 생략한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(OutboxPublisherProperties.class)
class OutboxPublisherConfiguration {

    @Bean
    OutboxPublishPolicy outboxPublishPolicy(OutboxPublisherProperties properties) {
        return new OutboxPublishPolicy(
                properties.batchSize(),
                properties.leaseDuration(),
                new ExponentialRetryBackoff(
                        properties.retryBaseDelay(),
                        properties.retryMaxDelay()
                )
        );
    }

    @Bean
    @ConditionalOnProperty(
            name = "spending-guard.outbox.publisher.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    NewTopic spendEventReceivedTopic(OutboxPublisherProperties properties) {
        return TopicBuilder.name(properties.topic())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
