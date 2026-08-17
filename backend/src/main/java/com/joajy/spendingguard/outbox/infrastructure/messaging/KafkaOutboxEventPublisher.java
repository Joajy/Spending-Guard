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

