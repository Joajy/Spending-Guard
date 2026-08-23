package com.joajy.spendingguard.account.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 사용자 계정 엔티티의 기본 저장과 식별자 조회를 제공한다. */
public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from UserAccountEntity account where account.id = :id")
    Optional<UserAccountEntity> findByIdForUpdate(@Param("id") UUID id);
}
