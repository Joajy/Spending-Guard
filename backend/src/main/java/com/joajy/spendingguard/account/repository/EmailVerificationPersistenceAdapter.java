package com.joajy.spendingguard.account.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import com.joajy.spendingguard.account.service.port.outbound.EmailVerificationStore;
import org.springframework.stereotype.Component;

@Component
class EmailVerificationPersistenceAdapter implements EmailVerificationStore {
    private final UserAccountJpaRepository accounts;
    private final EmailVerificationChallengeJpaRepository challenges;
    EmailVerificationPersistenceAdapter(UserAccountJpaRepository accounts, EmailVerificationChallengeJpaRepository challenges) {
        this.accounts = accounts; this.challenges = challenges;
    }
    public Optional<AccountTarget> findAccount(UUID userId) {
        return accounts.findByIdForUpdate(userId).map(a -> new AccountTarget(a.getId(), a.getEmail(), a.getEmailVerifiedAt() != null));
    }
    public void replaceChallenge(UUID userId, String codeHash, Instant expiresAt, Instant createdAt) {
        challenges.save(new EmailVerificationChallengeEntity(userId, codeHash, expiresAt, createdAt));
    }
    public Optional<Challenge> findChallenge(UUID userId) {
        return challenges.findByUserIdForUpdate(userId)
                .map(c -> new Challenge(c.getCodeHash(), c.getExpiresAt(), c.getCreatedAt(), c.getFailedAttempts()));
    }
    public int incrementFailedAttempts(UUID userId) {
        return challenges.findByUserIdForUpdate(userId)
                .map(EmailVerificationChallengeEntity::incrementFailedAttempts)
                .orElseThrow();
    }
    public boolean markVerifiedAndDeleteChallenge(UUID userId, Instant verifiedAt) {
        return accounts.findById(userId).map(a -> { a.verifyEmail(verifiedAt); challenges.deleteById(userId); return true; }).orElse(false);
    }
}
