package com.joajy.spendingguard.outbox.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Outbox 엔티티의 기본 저장과 조회를 제공하는 Spring Data 저장소다.
 *
 * <p><strong>포함 기능:</strong> 소비 이벤트 접수 트랜잭션 안에서 새
 * {@link OutboxEventEntity}를 저장하는 단순 영속성 연산을 제공한다.
 *
 * <p><strong>존재 이유:</strong> 최초 INSERT는 JPA가 제공하는 기본 연산으로 충분하지만,
 * 동시 선점과 조건부 상태 전이는 {@code SKIP LOCKED}, claim token 비교, 갱신 건수 확인이
 * 필요하다. 두 작업을 억지로 하나의 저장소에 넣지 않고 복잡한 SQL은
 * {@link OutboxDispatchPersistenceAdapter}에 맡겨 각 도구가 잘하는 범위를 분리한다.
 */
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {
}
