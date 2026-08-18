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
 *
 * <p><strong>도입 배경:</strong> 배치 크기나 재시도 간격 같은 운영값을 애플리케이션
 * 서비스가 직접 읽기 시작하면 발행 규칙이 Spring 설정 형식에 묶인다. 이 구성 클래스가
 * 외부 설정을 {@link OutboxPublishPolicy}로 변환해 서비스에는 실행에 필요한 정책만 전달한다.
 *
 * <p><strong>포함 기능:</strong> 타입 검증이 끝난 설정으로 지수 백오프 정책을 만들고,
 * 스케줄링을 활성화하며, 발행 기능이 켜진 환경에서만 Kafka 토픽을 선언한다.
 *
 * <p><strong>존재 이유:</strong> 운영 설정과 애플리케이션 규칙의 번역 지점을 한곳에
 * 두면 로컬·테스트·운영 환경의 값이 달라도 발행 서비스의 코드는 바뀌지 않는다.
 * 토픽 생성 조건도 발행 기능의 활성화 조건과 맞춰 불필요한 외부 의존성 초기화를 막는다.
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
