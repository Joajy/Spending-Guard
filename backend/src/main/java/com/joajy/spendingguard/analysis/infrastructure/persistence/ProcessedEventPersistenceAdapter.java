package com.joajy.spendingguard.analysis.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.analysis.application.port.outbound.TryClaimProcessedEventPort;
import org.springframework.stereotype.Component;

/** 처리 이력의 고유 제약을 멱등 Consumer 선점 연산으로 노출하는 영속성 어댑터다. */
@Component
class ProcessedEventPersistenceAdapter implements TryClaimProcessedEventPort {

    private final ProcessedEventJpaRepository repository;

    ProcessedEventPersistenceAdapter(ProcessedEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean tryClaim(UUID eventId, String consumerName, Instant processedAt) {
        return repository.claim(UUID.randomUUID(), eventId, consumerName, processedAt) == 1;
    }
}
