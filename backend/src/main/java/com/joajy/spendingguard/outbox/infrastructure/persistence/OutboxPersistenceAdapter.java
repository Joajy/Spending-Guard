package com.joajy.spendingguard.outbox.infrastructure.persistence;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joajy.spendingguard.spendevent.application.port.outbound.AppendSpendEventOutboxPort;
import com.joajy.spendingguard.spendevent.domain.event.SpendEventReceived;
import org.springframework.stereotype.Component;

@Component
class OutboxPersistenceAdapter implements AppendSpendEventOutboxPort {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    OutboxPersistenceAdapter(OutboxEventJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(SpendEventReceived event) {
        repository.save(new OutboxEventEntity(
                event.eventId(),
                createPayload(event),
                event.receivedAt()
        ));
    }

    private String createPayload(SpendEventReceived event) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "schemaVersion", 1,
                    "eventId", event.eventId(),
                    "source", event.source(),
                    "receivedAt", event.receivedAt()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("소비 이벤트 발행 데이터를 생성할 수 없습니다.", exception);
        }
    }
}
