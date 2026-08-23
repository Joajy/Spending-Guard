package com.joajy.spendingguard.account.repository;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface EmailVerificationChallengeJpaRepository extends JpaRepository<EmailVerificationChallengeEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select challenge from EmailVerificationChallengeEntity challenge where challenge.userId = :userId")
    Optional<EmailVerificationChallengeEntity> findByUserIdForUpdate(@Param("userId") UUID userId);
}
