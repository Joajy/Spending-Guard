package com.joajy.spendingguard.spendevent.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RawSpendEventRepository extends JpaRepository<RawSpendEvent, UUID> {
}
