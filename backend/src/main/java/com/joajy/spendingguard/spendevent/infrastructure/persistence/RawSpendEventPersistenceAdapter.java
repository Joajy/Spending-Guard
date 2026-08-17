package com.joajy.spendingguard.spendevent.infrastructure.persistence;

import com.joajy.spendingguard.spendevent.application.exception.DuplicateSpendEventException;
import com.joajy.spendingguard.spendevent.application.port.outbound.StoreRawSpendEventPort;
import com.joajy.spendingguard.spendevent.domain.model.RawSpendEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 원천 소비 이벤트 저장 포트를 JPA로 구현하는 영속성 어댑터다.
 * 즉시 flush해 중복 키의 고유 제약 위반을 현재 유스케이스 안에서 확인하고, 기술 예외를 중복 접수 예외로 변환한다.
 */
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
