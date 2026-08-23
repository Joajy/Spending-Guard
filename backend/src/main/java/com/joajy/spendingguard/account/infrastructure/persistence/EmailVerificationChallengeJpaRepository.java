package com.joajy.spendingguard.account.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EmailVerificationChallengeJpaRepository extends JpaRepository<EmailVerificationChallengeEntity, UUID> { }
