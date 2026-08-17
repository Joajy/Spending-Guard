package com.joajy.spendingguard.outbox.infrastructure.persistence;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joajy.spendingguard.spendevent.application.port.outbound.AppendSpendEventOutboxPort;
import com.joajy.spendingguard.spendevent.domain.event.SpendEventReceived;
import org.springframework.stereotype.Component;

/**
 * 접수된 소비 이벤트를 JSON 페이로드로 직렬화해 Outbox 테이블에 적재하는 출력 어댑터다.
 * 소비 원문 대신 후속 처리에 필요한 최소 식별 정보만 저장하며, 소비 이벤트 저장 트랜잭션에 참여해 두 기록의 원자성을 보장한다.
 */
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
