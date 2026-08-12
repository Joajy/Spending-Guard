package com.joajy.spendingguard.spendevent.persistence;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.SpendEventSource;
import com.joajy.spendingguard.spendevent.domain.SpendEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "raw_spend_event")
public class RawSpendEvent {

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

    public RawSpendEvent(
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

    public UUID getId() {
        return id;
    }

    public SpendEventSource getSource() {
        return source;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }

    public String getSanitizedMessage() {
        return sanitizedMessage;
    }

    public SpendEventStatus getStatus() {
        return status;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
