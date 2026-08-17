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

