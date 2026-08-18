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
 *
 * <p>업무 데이터와 같은 트랜잭션에서 {@code PENDING} 상태로 생성된다. 발행 시도 횟수,
 * 다음 시도 시각, 임대 토큰과 만료 시각, 마지막 오류 코드를 보관해 프로세스 재시작
 * 이후에도 발행 수명주기를 이어간다.
 *
 * <p>도메인 모델이 아닌 영속성 전용 모델이다. 상태 전이는 동시성 조건을 SQL에 함께
 * 표현해야 하므로 엔티티 변경 메서드가 아니라 dispatch 어댑터가 담당한다.
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

    /** JPA 프록시와 리플렉션 기반 생성을 위한 생성자. */
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
