package com.joajy.spendingguard.analysis.messaging;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.joajy.spendingguard.analysis.service.command.ProcessSpendEventCommand;
import com.joajy.spendingguard.analysis.service.port.inbound.ProcessSpendEventUseCase;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SpendEventKafkaListenerTest {

    private final ProcessSpendEventUseCase useCase = mock(ProcessSpendEventUseCase.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final SpendEventKafkaListener listener = new SpendEventKafkaListener(
            objectMapper,
            useCase,
            meterRegistry
    );

    @Test
    void forwardsVersionOneMessageToProcessingUseCase() {
        UUID eventId = UUID.randomUUID();
        String payload = """
                {
                  "schemaVersion": 1,
                  "eventId": "%s",
                  "source": "MANUAL_TEXT",
                  "receivedAt": "%s"
                }
                """.formatted(eventId, Instant.parse("2026-08-18T01:00:00Z"));

        listener.consume(payload);

        verify(useCase).process(new ProcessSpendEventCommand(eventId));
        assertThat(meterRegistry.counter("spending.guard.analysis.consume.success").count())
                .isOne();
        assertThat(meterRegistry.timer("spending.guard.analysis.consume.duration").count())
                .isOne();
    }

    @Test
    void rejectsUnsupportedSchemaWithoutCallingUseCase() {
        String payload = """
                {
                  "schemaVersion": 2,
                  "eventId": "%s",
                  "source": "SIMULATOR",
                  "receivedAt": "2026-08-18T01:00:00Z"
                }
                """.formatted(UUID.randomUUID());

        assertThatThrownBy(() -> listener.consume(payload))
                .isInstanceOf(InvalidSpendEventMessageException.class);
        assertThat(meterRegistry.counter("spending.guard.analysis.consume.failure").count())
                .isOne();
    }
}
