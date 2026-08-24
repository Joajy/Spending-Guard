package com.joajy.spendingguard.auth.service.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.auth.service.model.RefreshTokenOwner;
import com.joajy.spendingguard.auth.service.result.RefreshToken;

public interface RefreshTokenStore {
    RefreshToken create(UUID userId, Instant issuedAt);

    Optional<RefreshTokenOwner> rotate(String token, Instant rotatedAt);

    void revoke(String token, Instant revokedAt);
}
