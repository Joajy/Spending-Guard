package com.joajy.spendingguard.outbox.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.joajy.spendingguard.outbox.application.exception.OutboxClaimLostException;
import com.joajy.spendingguard.outbox.application.model.ClaimedOutboxEvent;
import com.joajy.spendingguard.outbox.application.port.outbound.ClaimOutboxEventsPort;
import com.joajy.spendingguard.outbox.application.port.outbound.UpdateOutboxEventStatePort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class OutboxDispatchPersistenceAdapter implements ClaimOutboxEventsPort, UpdateOutboxEventStatePort {

    private static final String CLAIM_SQL = """
            WITH candidates AS (
                SELECT id
                FROM outbox_event
                WHERE (status = 'PENDING' AND next_attempt_at <= :claimedAt)
                   OR (status = 'PROCESSING' AND claimed_until <= :claimedAt)
                ORDER BY created_at
                LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox_event AS event
            SET status = 'PROCESSING',
                claim_token = :claimToken,
                claimed_until = :claimedUntil,
                last_error_code = NULL
            FROM candidates
            WHERE event.id = candidates.id
            RETURNING event.id,
                      event.aggregate_id,
                      event.event_type,
                      CAST(event.payload AS text) AS payload,
                      event.attempt_count,
                      event.claim_token
            """;

    private static final String MARK_PUBLISHED_SQL = """
            UPDATE outbox_event
            SET status = 'PUBLISHED',
                published_at = :publishedAt,
                claim_token = NULL,
                claimed_until = NULL,
                last_error_code = NULL
            WHERE id = :eventId
              AND status = 'PROCESSING'
              AND claim_token = :claimToken
            """;

    private static final String MARK_FAILED_SQL = """
            UPDATE outbox_event
            SET status = 'PENDING',
                attempt_count = attempt_count + 1,
                next_attempt_at = :nextAttemptAt,
                claim_token = NULL,
                claimed_until = NULL,
                last_error_code = :errorCode
            WHERE id = :eventId
              AND status = 'PROCESSING'
              AND claim_token = :claimToken
            """;

    private final JdbcClient jdbcClient;

    OutboxDispatchPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClaimedOutboxEvent> claim(
            int batchSize,
            Instant claimedAt,
            Instant claimedUntil
    ) {
        UUID claimToken = UUID.randomUUID();
        return jdbcClient.sql(CLAIM_SQL)
                .param("batchSize", batchSize)
                .param("claimedAt", toDatabaseTimestamp(claimedAt))
                .param("claimedUntil", toDatabaseTimestamp(claimedUntil))
                .param("claimToken", claimToken)
                .query(this::mapClaimedEvent)
                .list();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID eventId, UUID claimToken, Instant publishedAt) {
        int updated = jdbcClient.sql(MARK_PUBLISHED_SQL)
                .param("eventId", eventId)
                .param("claimToken", claimToken)
                .param("publishedAt", toDatabaseTimestamp(publishedAt))
                .update();
        requireClaim(updated, eventId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(
            UUID eventId,
            UUID claimToken,
            Instant nextAttemptAt,
            String errorCode
    ) {
        int updated = jdbcClient.sql(MARK_FAILED_SQL)
                .param("eventId", eventId)
                .param("claimToken", claimToken)
                .param("nextAttemptAt", toDatabaseTimestamp(nextAttemptAt))
                .param("errorCode", errorCode)
                .update();
        requireClaim(updated, eventId);
    }

    private ClaimedOutboxEvent mapClaimedEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ClaimedOutboxEvent(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("aggregate_id", UUID.class),
                resultSet.getString("event_type"),
                resultSet.getString("payload"),
                resultSet.getInt("attempt_count"),
                resultSet.getObject("claim_token", UUID.class)
        );
    }

    private OffsetDateTime toDatabaseTimestamp(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private void requireClaim(int updated, UUID eventId) {
        if (updated != 1) {
            throw new OutboxClaimLostException(eventId);
        }
    }

}

