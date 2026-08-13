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
