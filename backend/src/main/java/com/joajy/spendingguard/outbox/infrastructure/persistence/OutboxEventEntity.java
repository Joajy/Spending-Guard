package com.joajy.spendingguard.outbox.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 외부 발행을 기다리는 이벤트와 처리 상태를 {@code outbox_event} 테이블에 매핑하는 JPA 엔티티다.
 * 업무 데이터와 같은 트랜잭션에서 처음 생성되며, 발행 시도 횟수·임대 정보·마지막 오류를 보관해 장애 후에도 처리를 이어갈 수 있게 한다.
 * 도메인 모델이 아닌 영속성 전용 모델이므로 생성 규칙과 컬럼 세부 사항은 인프라 계층 안에 둔다.
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEventEntity {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "claim_token")
    private UUID claimToken;

    @Column(name = "claimed_until")
    private Instant claimedUntil;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    protected OutboxEventEntity() {
    }

    OutboxEventEntity(UUID aggregateId, String payload, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.aggregateType = "RawSpendEvent";
        this.aggregateId = aggregateId;
        this.eventType = "SpendEventReceived";
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.attemptCount = 0;
        this.createdAt = createdAt;
        this.nextAttemptAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public String getStatus() {
        return status.name();
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public UUID getClaimToken() {
        return claimToken;
    }

    public Instant getClaimedUntil() {
        return claimedUntil;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }
}
