package com.joajy.spendingguard.analysis.config;

import com.joajy.spendingguard.analysis.domain.policy.FastSpendEventParser;
import com.joajy.spendingguard.analysis.messaging.InvalidSpendEventMessageException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** 빠른 파서와 Kafka 재시도·DLQ 정책을 애플리케이션 실행 환경에 조립한다. */
@Configuration
@EnableConfigurationProperties(SpendEventConsumerProperties.class)
class SpendEventAnalysisConfiguration {

    @Bean
    FastSpendEventParser fastSpendEventParser() {
        return new FastSpendEventParser();
    }

    @Bean
    DefaultErrorHandler spendEventConsumerErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            SpendEventConsumerProperties properties
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(
                        properties.deadLetterTopic(),
                        record.partition()
                )
        );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(
                        properties.retryBackoff().toMillis(),
                        properties.maxRetries()
                )
        );
        errorHandler.addNotRetryableExceptions(InvalidSpendEventMessageException.class);
        return errorHandler;
    }
}
