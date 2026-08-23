package com.joajy.spendingguard.outbox.messaging;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.joajy.spendingguard.outbox.service.exception.OutboxPublishException;
import com.joajy.spendingguard.outbox.service.model.ClaimedOutboxEvent;
import com.joajy.spendingguard.outbox.config.OutboxPublisherProperties;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class KafkaOutboxEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void convertsKafkaFailureIntoRetryablePublishFailure() {
        ClaimedOutboxEvent event = claimedEvent();
        CompletableFuture<SendResult<String, String>> failedSend = new CompletableFuture<>();
        failedSend.completeExceptionally(new KafkaException("broker unavailable"));
        given(kafkaTemplate.send("spend-event.received.v1", event.aggregateId().toString(), event.payload()))
                .willReturn(failedSend);
        KafkaOutboxEventPublisher publisher = new KafkaOutboxEventPublisher(
                kafkaTemplate,
                properties(Duration.ofSeconds(1))
        );

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOfSatisfying(OutboxPublishException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("KAFKA_SEND_FAILED");
                    assertThat(exception.getCause()).isInstanceOf(KafkaException.class);
                });
    }

    private OutboxPublisherProperties properties(Duration sendTimeout) {
        return new OutboxPublisherProperties(
                "spend-event.received.v1",
                20,
                Duration.ofSeconds(30),
                Duration.ofSeconds(5),
                Duration.ofMinutes(5),
                sendTimeout
        );
    }

    private ClaimedOutboxEvent claimedEvent() {
        return new ClaimedOutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SpendEventReceived",
                "{\"schemaVersion\":1}",
                0,
                UUID.randomUUID()
        );
    }
}

