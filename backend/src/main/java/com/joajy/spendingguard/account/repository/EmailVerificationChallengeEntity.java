package com.joajy.spendingguard.account.repository;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "email_verification_challenge")
class EmailVerificationChallengeEntity {
    @Id @Column(name = "user_id") private UUID userId;
    @Column(name = "code_hash", nullable = false, length = 100) private String codeHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "failed_attempts", nullable = false) private int failedAttempts;

    protected EmailVerificationChallengeEntity() { }
    EmailVerificationChallengeEntity(UUID userId, String codeHash, Instant expiresAt, Instant createdAt) {
        this.userId = userId; this.codeHash = codeHash; this.expiresAt = expiresAt; this.createdAt = createdAt;
        this.failedAttempts = 0;
    }
    String getCodeHash() { return codeHash; }
    Instant getExpiresAt() { return expiresAt; }
    Instant getCreatedAt() { return createdAt; }
    int getFailedAttempts() { return failedAttempts; }
    int incrementFailedAttempts() { return ++failedAttempts; }
}
