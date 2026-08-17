package com.joajy.spendingguard.spendevent.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.RawSpendEvent;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 원천 소비 이벤트를 {@code raw_spend_event} 테이블에 매핑하는 JPA 엔티티다.
 * 도메인 모델을 영속성 표현으로 변환하고, 중복 키의 고유 제약으로 동시에 들어온 동일 알림도 데이터베이스에서 한 번만 허용한다.
 * 저장되는 메시지는 사전에 정제된 값이며 원본 금융 알림을 그대로 보관하지 않는다.
 */
@Entity
@Table(name = "raw_spend_event")
public class RawSpendEventEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SpendEventSource source;

    @Column(name = "external_event_id", length = 200)
    private String externalEventId;

    @Column(name = "deduplication_key", nullable = false, length = 64, unique = true)
    private String deduplicationKey;

    @Column(name = "sanitized_message", nullable = false, length = 2000)
    private String sanitizedMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SpendEventStatus status;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected RawSpendEventEntity() {
    }

    private RawSpendEventEntity(RawSpendEvent spendEvent) {
        this.id = spendEvent.id();
        this.source = spendEvent.source();
        this.externalEventId = spendEvent.externalEventId();
        this.deduplicationKey = spendEvent.deduplicationKey();
        this.sanitizedMessage = spendEvent.sanitizedMessage();
        this.status = spendEvent.status();
        this.occurredAt = spendEvent.occurredAt();
        this.receivedAt = spendEvent.receivedAt();
    }

    static RawSpendEventEntity from(RawSpendEvent spendEvent) {
        return new RawSpendEventEntity(spendEvent);
    }

    public UUID getId() {
        return id;
    }

    public String getSanitizedMessage() {
        return sanitizedMessage;
    }
}
