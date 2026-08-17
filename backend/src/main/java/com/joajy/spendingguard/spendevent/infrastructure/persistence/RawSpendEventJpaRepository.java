package com.joajy.spendingguard.spendevent.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 원천 소비 이벤트 엔티티의 기본 영속성 연산을 제공하는 Spring Data 저장소다.
 * 애플리케이션 포트의 구현 세부 사항으로만 사용되어 상위 계층에 JPA 의존성을 노출하지 않는다.
 */
public interface RawSpendEventJpaRepository extends JpaRepository<RawSpendEventEntity, UUID> {
}
