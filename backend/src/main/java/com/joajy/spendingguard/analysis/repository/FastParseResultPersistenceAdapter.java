package com.joajy.spendingguard.analysis.repository;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.analysis.service.port.outbound.StoreFastParseResultPort;
import com.joajy.spendingguard.analysis.domain.model.FastParseOutcome;
import org.springframework.stereotype.Component;

/** 빠른 분석 결과를 원천 이벤트 식별자로 저장하는 영속성 출력 어댑터다. */
@Component
class FastParseResultPersistenceAdapter implements StoreFastParseResultPort {

    private final FastParseResultJpaRepository repository;

    FastParseResultPersistenceAdapter(FastParseResultJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void store(
            UUID eventId,
            FastParseOutcome outcome,
            String parserVersion,
            Instant parsedAt
    ) {
        repository.save(new FastParseResultEntity(
                eventId,
                outcome,
                parserVersion,
                parsedAt
        ));
    }
}
