package com.joajy.spendingguard.analysis.messaging;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joajy.spendingguard.analysis.repository.FastParseResultJpaRepository;
import com.joajy.spendingguard.analysis.repository.ProcessedEventJpaRepository;
import com.joajy.spendingguard.outbox.repository.OutboxEventJpaRepository;
import com.joajy.spendingguard.spendevent.service.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.service.result.SpendEventReceipt;
import com.joajy.spendingguard.spendevent.service.SpendEventService;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.repository.RawSpendEventJpaRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(properties = {
        "spending-guard.outbox.publisher.enabled=false",
        "spending-guard.analysis.consumer.enabled=true",
        "spending-guard.analysis.consumer.topic="
                + SpendEventKafkaListenerIntegrationTest.TOPIC,
        "spending-guard.analysis.consumer.group-id=spend-event-listener-integration",
        "spending-guard.analysis.consumer.retry-backoff=10ms",
        "spending-guard.analysis.consumer.dead-letter-topic="
                + SpendEventKafkaListenerIntegrationTest.DEAD_LETTER_TOPIC
})
@EmbeddedKafka(
        partitions = 1,
        topics = {
                SpendEventKafkaListenerIntegrationTest.TOPIC,
                SpendEventKafkaListenerIntegrationTest.DEAD_LETTER_TOPIC
        },
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
class SpendEventKafkaListenerIntegrationTest {

    static final String TOPIC = "spend-event.received.v1-listener-test";
    static final String DEAD_LETTER_TOPIC = TOPIC + ".DLT";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SpendEventService spendEventService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private FastParseResultJpaRepository fastParseResultRepository;

    @Autowired
    private ProcessedEventJpaRepository processedEventRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventRepository;

    @Autowired
    private RawSpendEventJpaRepository rawSpendEventRepository;

    @BeforeEach
    void cleanDatabase() {
        fastParseResultRepository.deleteAll();
        processedEventRepository.deleteAll();
        outboxEventRepository.deleteAll();
        rawSpendEventRepository.deleteAll();
    }

    @Test
    void consumesRedeliveryFromKafkaWithoutDuplicatingBusinessWrites() throws Exception {
        SpendEventReceipt first = submit("쿠팡 12,800원 결제", "kafka-redelivery-100");
        SpendEventReceipt marker = submit("카카오T 28,000원 승인", "kafka-marker-100");

        publish(first);
        publish(first);
        publish(marker);
        awaitParseResult(marker.eventId(), Duration.ofSeconds(10));

        assertThat(fastParseResultRepository.findById(first.eventId())).isPresent();
        assertThat(fastParseResultRepository.count()).isEqualTo(2);
        assertThat(processedEventRepository.count()).isEqualTo(2);
    }

    @Test
    void routesUnsupportedSchemaToDeadLetterTopic() throws Exception {
        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(
                "spend-event-dlt-verification-" + UUID.randomUUID(),
                "true",
                broker
        );
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProperties,
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, DEAD_LETTER_TOPIC);
            String invalidPayload = """
                    {
                      "schemaVersion": 2,
                      "eventId": "%s",
                      "source": "SIMULATOR",
                      "receivedAt": "2026-08-18T01:00:00Z"
                    }
                    """.formatted(UUID.randomUUID());

            kafkaTemplate.send(TOPIC, "invalid-schema", invalidPayload)
                    .get(10, TimeUnit.SECONDS);

            ConsumerRecord<String, String> deadLetter = KafkaTestUtils.getSingleRecord(
                    consumer,
                    DEAD_LETTER_TOPIC,
                    Duration.ofSeconds(10)
            );
            assertThat(deadLetter.key()).isEqualTo("invalid-schema");
            assertThat(deadLetter.value()).isEqualTo(invalidPayload);
        }
    }

    private SpendEventReceipt submit(String message, String externalEventId) {
        return spendEventService.submit(new SubmitSpendEventCommand(
                SpendEventSource.SIMULATOR,
                externalEventId,
                message,
                Instant.parse("2026-08-18T01:00:00Z")
        ));
    }

    private void publish(SpendEventReceipt receipt) throws Exception {
        kafkaTemplate.send(
                TOPIC,
                receipt.eventId().toString(),
                payload(receipt)
        ).get(10, TimeUnit.SECONDS);
    }

    private String payload(SpendEventReceipt receipt) throws JsonProcessingException {
        return objectMapper.writeValueAsString(Map.of(
                "schemaVersion", 1,
                "eventId", receipt.eventId(),
                "source", SpendEventSource.SIMULATOR,
                "receivedAt", receipt.receivedAt()
        ));
    }

    private void awaitParseResult(UUID eventId, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (fastParseResultRepository.existsById(eventId)) {
                return;
            }
            Thread.sleep(100);
        }
        fail("Kafka 소비 결과가 제한 시간 안에 저장되지 않았습니다: " + eventId);
    }
}
