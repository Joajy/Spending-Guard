package com.joajy.spendingguard.spendevent;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RawSpendEventRepository extends JpaRepository<RawSpendEvent, UUID> {
}
