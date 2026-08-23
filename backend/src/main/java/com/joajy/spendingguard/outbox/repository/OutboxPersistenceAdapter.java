package com.joajy.spendingguard.outbox.repository;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joajy.spendingguard.spendevent.service.port.outbound.AppendSpendEventOutboxPort;
import com.joajy.spendingguard.spendevent.domain.event.SpendEventReceived;
import org.springframework.stereotype.Component;

/**
 * 접수된 소비 이벤트를 JSON 페이로드로 직렬화해 Outbox 테이블에 적재하는 출력 어댑터다.
 *
 * <p><strong>원자성:</strong> 별도 트랜잭션을 열지 않고 소비 이벤트 접수 트랜잭션에
 * 참여한다. 원천 이벤트와 Outbox 행 중 하나라도 저장하지 못하면 둘 다 롤백된다.
 *
 * <p><strong>페이로드 최소화:</strong> 소비 원문 대신 스키마 버전, 이벤트 ID, 유입 경로,
 * 접수 시각만 JSON으로 저장한다. 후속 소비자는 이벤트 ID로 정제된 원천 데이터를 조회한다.
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
