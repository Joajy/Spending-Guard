package com.joajy.spendingguard.analysis.repository;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Kafka 재전달 시 같은 Consumer의 업무 반영을 한 번으로 제한하는 선점 기록이다. */
@Entity
@Table(name = "processed_event")
public class ProcessedEventEntity {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEventEntity() {
    }
}
