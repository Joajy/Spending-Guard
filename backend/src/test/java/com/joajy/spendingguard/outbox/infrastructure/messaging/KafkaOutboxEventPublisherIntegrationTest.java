package com.joajy.spendingguard.outbox.infrastructure.messaging;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import com.joajy.spendingguard.outbox.application.model.ClaimedOutboxEvent;
import com.joajy.spendingguard.outbox.infrastructure.config.OutboxPublisherProperties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(partitions = 1, topics = KafkaOutboxEventPublisherIntegrationTest.TOPIC)
class KafkaOutboxEventPublisherIntegrationTest {

    static final String TOPIC = "spend-event.received.v1-test";

    @Test
    void publishesPayloadWithAggregateIdAsKafkaKey(EmbeddedKafkaBroker broker) {
        Map<String, Object> producerProperties = KafkaTestUtils.producerProps(broker);
        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(
                producerProperties,
                new StringSerializer(),
                new StringSerializer()
        );
        KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        KafkaOutboxEventPublisher publisher = new KafkaOutboxEventPublisher(
                kafkaTemplate,
                properties()
        );
        ClaimedOutboxEvent event = claimedEvent();

        try {
            publisher.publish(event);

            Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(
                    "outbox-publisher-test",
                    "true",
                    broker
            );
            try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                    consumerProperties,
                    new StringDeserializer(),
                    new StringDeserializer()
            ).createConsumer()) {
                broker.consumeFromAnEmbeddedTopic(consumer, TOPIC);
                ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(
                        consumer,
                        TOPIC,
                        Duration.ofSeconds(10)
                );

                assertThat(record.key()).isEqualTo(event.aggregateId().toString());
                assertThat(record.value()).isEqualTo(event.payload());
            }
        } finally {
            producerFactory.destroy();
        }
    }

    private OutboxPublisherProperties properties() {
        return new OutboxPublisherProperties(
                TOPIC,
                20,
                Duration.ofSeconds(30),
                Duration.ofSeconds(5),
                Duration.ofMinutes(5),
                Duration.ofSeconds(5)
        );
    }

    private ClaimedOutboxEvent claimedEvent() {
        return new ClaimedOutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SpendEventReceived",
                "{\"schemaVersion\":1,\"eventType\":\"SpendEventReceived\"}",
                0,
                UUID.randomUUID()
        );
    }
}

