package com.joajy.spendingguard.spendevent.repository;

import com.joajy.spendingguard.spendevent.service.exception.DuplicateSpendEventException;
import com.joajy.spendingguard.spendevent.service.port.outbound.StoreRawSpendEventPort;
import com.joajy.spendingguard.spendevent.domain.model.RawSpendEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 원천 소비 이벤트 저장 포트를 JPA로 구현하는 영속성 어댑터다.
 *
 * <p>{@code saveAndFlush}로 INSERT와 고유 제약 검사를 현재 유스케이스 안에서 실행한다.
 * 커밋 시점까지 오류를 미루지 않으므로 PostgreSQL의 중복 키 위반을 즉시
 * {@link DuplicateSpendEventException}으로 변환할 수 있다.
 *
 * <p>중복 판정의 최종 기준은 {@code deduplication_key} 고유 제약이다. 애플리케이션의
 * 조회-후-저장 경쟁 조건 없이 동시에 도착한 요청 중 하나만 성공한다.
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
