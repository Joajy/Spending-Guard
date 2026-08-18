package com.joajy.spendingguard.analysis.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** PostgreSQL의 원자적 충돌 처리를 이용해 Consumer 선점 결과를 반환한다. */
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_event (id, event_id, consumer_name, processed_at)
            VALUES (:id, :eventId, :consumerName, :processedAt)
            ON CONFLICT (event_id, consumer_name) DO NOTHING
            """, nativeQuery = true)
    int claim(
            @Param("id") UUID id,
            @Param("eventId") UUID eventId,
            @Param("consumerName") String consumerName,
            @Param("processedAt") Instant processedAt
    );
}
