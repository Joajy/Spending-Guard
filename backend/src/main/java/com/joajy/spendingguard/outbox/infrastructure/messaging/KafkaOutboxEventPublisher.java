package com.joajy.spendingguard.outbox.infrastructure.messaging;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.joajy.spendingguard.outbox.application.exception.OutboxPublishException;
import com.joajy.spendingguard.outbox.application.model.ClaimedOutboxEvent;
import com.joajy.spendingguard.outbox.application.port.outbound.PublishOutboxEventPort;
import com.joajy.spendingguard.outbox.infrastructure.config.OutboxPublisherProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Outbox 이벤트를 Kafka로 전달하는 메시징 출력 어댑터다.
 * 같은 소비 이벤트의 순서를 유지할 수 있도록 aggregate ID를 메시지 키로 사용하고, 제한 시간 안에 브로커 확인을 받아야 성공으로 반환한다.
 * 중단, 시간 초과, 전송 실패를 구분된 오류 코드로 변환해 애플리케이션의 재시도 판단에 전달한다.
 */
@Component
class KafkaOutboxEventPublisher implements PublishOutboxEventPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxPublisherProperties properties;

    KafkaOutboxEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            OutboxPublisherProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(ClaimedOutboxEvent event) {
        try {
            kafkaTemplate.send(
                    properties.topic(),
                    event.aggregateId().toString(),
                    event.payload()
            ).get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OutboxPublishException("KAFKA_SEND_INTERRUPTED", exception);
        } catch (TimeoutException exception) {
            throw new OutboxPublishException("KAFKA_SEND_TIMEOUT", exception);
        } catch (ExecutionException exception) {
            throw new OutboxPublishException("KAFKA_SEND_FAILED", exception.getCause());
        }
    }
}
