package com.joajy.spendingguard.analysis.messaging;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.joajy.spendingguard.analysis.service.command.ProcessSpendEventCommand;
import com.joajy.spendingguard.analysis.service.port.inbound.ProcessSpendEventUseCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SpendEventKafkaListenerTest {

    private final ProcessSpendEventUseCase useCase = mock(ProcessSpendEventUseCase.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final SpendEventKafkaListener listener = new SpendEventKafkaListener(objectMapper, useCase);

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
    }
}
