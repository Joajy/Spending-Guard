package com.joajy.spendingguard.outbox.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
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

    private static final String SELECT_CLAIM_CANDIDATES_SQL = """
            SELECT id,
                   aggregate_id,
                   event_type,
                   CAST(payload AS text) AS payload,
                   attempt_count
            FROM outbox_event
            WHERE (status = 'PENDING' AND next_attempt_at <= :claimedAt)
               OR (status = 'PROCESSING' AND claimed_until <= :claimedAt)
            ORDER BY created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """;

    private static final String CLAIM_CANDIDATES_SQL = """
            UPDATE outbox_event
            SET status = 'PROCESSING',
                claim_token = :claimToken,
                claimed_until = :claimedUntil,
                last_error_code = NULL
            WHERE id IN (:eventIds)
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
        List<OutboxClaimCandidate> candidates = jdbcClient.sql(SELECT_CLAIM_CANDIDATES_SQL)
                .param("batchSize", batchSize)
                .param("claimedAt", claimedAt)
                .query(this::mapClaimCandidate)
                .list();
        if (candidates.isEmpty()) {
            return List.of();
        }

        UUID claimToken = UUID.randomUUID();
        int updated = jdbcClient.sql(CLAIM_CANDIDATES_SQL)
                .param("eventIds", candidates.stream().map(OutboxClaimCandidate::id).toList())
                .param("claimedUntil", claimedUntil)
                .param("claimToken", claimToken)
                .update();
        if (updated != candidates.size()) {
            throw new IllegalStateException("Outbox claim update count does not match selected candidates");
        }

        return candidates.stream()
                .map(candidate -> candidate.claimedWith(claimToken))
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID eventId, UUID claimToken, Instant publishedAt) {
        int updated = jdbcClient.sql(MARK_PUBLISHED_SQL)
                .param("eventId", eventId)
                .param("claimToken", claimToken)
                .param("publishedAt", publishedAt)
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
                .param("nextAttemptAt", nextAttemptAt)
                .param("errorCode", errorCode)
                .update();
        requireClaim(updated, eventId);
    }

    private OutboxClaimCandidate mapClaimCandidate(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OutboxClaimCandidate(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("aggregate_id", UUID.class),
                resultSet.getString("event_type"),
                resultSet.getString("payload"),
                resultSet.getInt("attempt_count")
        );
    }

    private void requireClaim(int updated, UUID eventId) {
        if (updated != 1) {
            throw new OutboxClaimLostException(eventId);
        }
    }

    private record OutboxClaimCandidate(
            UUID id,
            UUID aggregateId,
            String eventType,
            String payload,
            int attemptCount
    ) {

        ClaimedOutboxEvent claimedWith(UUID claimToken) {
            return new ClaimedOutboxEvent(
                    id,
                    aggregateId,
                    eventType,
                    payload,
                    attemptCount,
                    claimToken
            );
        }
    }
}

