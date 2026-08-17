package com.joajy.spendingguard.outbox.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Outbox 엔티티의 기본 저장과 조회를 제공하는 Spring Data 저장소다.
 * 이벤트 최초 적재에 사용하며, 동시 선점처럼 데이터베이스 기능이 필요한 작업은 별도 JDBC 어댑터가 담당한다.
 */
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {
}
