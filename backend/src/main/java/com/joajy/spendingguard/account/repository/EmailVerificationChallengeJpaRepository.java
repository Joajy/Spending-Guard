package com.joajy.spendingguard.account.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EmailVerificationChallengeJpaRepository extends JpaRepository<EmailVerificationChallengeEntity, UUID> { }
