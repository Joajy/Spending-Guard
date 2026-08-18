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
 *
 * <p><strong>포함 기능:</strong> 도메인 모델을 컬럼 형태로 변환하고, 수집 채널·외부
 * 식별자·중복 키·정제 메시지·처리 상태와 두 종류의 시각을 저장한다. 중복 키에는 고유
 * 제약을 적용해 동시에 들어온 동일 알림도 데이터베이스가 한 건만 허용하게 한다.
 *
 * <p><strong>존재 이유:</strong> 도메인 모델과 영속성 모델을 분리하면 테이블 컬럼,
 * 문자열 길이, JPA 생성 규칙이 업무 모델로 퍼지지 않는다. 저장소 변경이 도메인 계약의
 * 변경으로 이어지는 것도 막는다.
 *
 * <p><strong>데이터 경계:</strong> {@code sanitizedMessage}에는 저장 전 정책을 통과한
 * 텍스트만 들어가며 금융 알림 원문을 별도로 보관하지 않는다. 다만 정제 정책이 모든
 * 개인정보를 탐지한다고 보장하지 않으므로 실제 연동 전 채널별 규칙이 추가되어야 한다.
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

    /** JPA 프록시와 리플렉션 기반 생성을 위한 생성자. */
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
