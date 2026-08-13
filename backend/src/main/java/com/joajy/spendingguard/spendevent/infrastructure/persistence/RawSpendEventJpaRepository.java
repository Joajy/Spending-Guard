package com.joajy.spendingguard.spendevent.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RawSpendEventJpaRepository extends JpaRepository<RawSpendEventEntity, UUID> {
}
