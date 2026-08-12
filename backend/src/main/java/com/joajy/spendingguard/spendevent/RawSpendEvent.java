package com.joajy.spendingguard.spendevent;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "raw_spend_event")
class RawSpendEvent {

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

    protected RawSpendEvent() {
    }

    RawSpendEvent(
            UUID id,
            SpendEventSource source,
            String externalEventId,
            String deduplicationKey,
            String sanitizedMessage,
            SpendEventStatus status,
            Instant occurredAt,
            Instant receivedAt
    ) {
        this.id = id;
        this.source = source;
        this.externalEventId = externalEventId;
        this.deduplicationKey = deduplicationKey;
        this.sanitizedMessage = sanitizedMessage;
        this.status = status;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
    }

    UUID getId() {
        return id;
    }

    SpendEventSource getSource() {
        return source;
    }

    String getExternalEventId() {
        return externalEventId;
    }

    String getDeduplicationKey() {
        return deduplicationKey;
    }

    String getSanitizedMessage() {
        return sanitizedMessage;
    }

    SpendEventStatus getStatus() {
        return status;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }

    Instant getReceivedAt() {
        return receivedAt;
    }
}
