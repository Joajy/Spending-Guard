package com.joajy.spendingguard.account.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자 계정 엔티티의 기본 저장과 식별자 조회를 제공한다. */
public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, UUID> {
}
