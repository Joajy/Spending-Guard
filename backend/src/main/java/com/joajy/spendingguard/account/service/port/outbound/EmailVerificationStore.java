package com.joajy.spendingguard.account.service.port.outbound;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationStore {
    Optional<AccountTarget> findAccount(UUID userId);

    void replaceChallenge(UUID userId, String codeHash, Instant expiresAt, Instant createdAt);

    Optional<Challenge> findChallenge(UUID userId);

    boolean markVerifiedAndDeleteChallenge(UUID userId, Instant verifiedAt);

    record AccountTarget(UUID userId, String email, boolean verified) {
    }

    record Challenge(String codeHash, Instant expiresAt) {
    }
}
