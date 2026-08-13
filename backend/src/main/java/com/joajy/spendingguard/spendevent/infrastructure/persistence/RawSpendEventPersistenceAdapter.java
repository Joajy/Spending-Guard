package com.joajy.spendingguard.spendevent.infrastructure.persistence;

import com.joajy.spendingguard.spendevent.application.exception.DuplicateSpendEventException;
import com.joajy.spendingguard.spendevent.application.port.outbound.StoreRawSpendEventPort;
import com.joajy.spendingguard.spendevent.domain.model.RawSpendEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
class RawSpendEventPersistenceAdapter implements StoreRawSpendEventPort {

    private final RawSpendEventJpaRepository repository;

    RawSpendEventPersistenceAdapter(RawSpendEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void store(RawSpendEvent spendEvent) {
        try {
            repository.saveAndFlush(RawSpendEventEntity.from(spendEvent));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateSpendEventException();
        }
    }
}
