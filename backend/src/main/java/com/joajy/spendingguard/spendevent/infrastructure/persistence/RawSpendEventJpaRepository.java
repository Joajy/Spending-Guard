package com.joajy.spendingguard.spendevent.infrastructure.persistence;

import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 원천 소비 이벤트 엔티티의 기본 영속성 연산을 제공하는 Spring Data 저장소다.
 *
 * <p><strong>포함 기능:</strong> {@link RawSpendEventEntity}의 저장과 식별자 기반 조회에
 * 필요한 기본 연산을 Spring Data로 제공한다.
 *
 * <p><strong>존재 이유:</strong> 이 인터페이스는
 * {@link RawSpendEventPersistenceAdapter} 내부에서만 사용한다. 애플리케이션 서비스가
 * JPA 저장소에 직접 의존하지 않게 해 유스케이스는 {@code StoreRawSpendEventPort}의
 * 도메인 계약만 알며, 기술 예외 변환과 flush 시점은 어댑터 한곳에서 통제한다.
 */
public interface RawSpendEventJpaRepository extends JpaRepository<RawSpendEventEntity, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("update RawSpendEventEntity event set event.status = :status where event.id = :eventId")
    int updateStatus(
            @Param("eventId") UUID eventId,
            @Param("status") SpendEventStatus status
    );
}
