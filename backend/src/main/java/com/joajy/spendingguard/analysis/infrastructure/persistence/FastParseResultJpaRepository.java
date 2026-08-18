package com.joajy.spendingguard.analysis.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** 빠른 분석 결과의 저장과 식별자 조회를 제공한다. */
public interface FastParseResultJpaRepository extends JpaRepository<FastParseResultEntity, UUID> {
}
