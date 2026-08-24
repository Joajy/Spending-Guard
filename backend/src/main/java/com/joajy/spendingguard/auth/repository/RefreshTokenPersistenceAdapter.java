package com.joajy.spendingguard.auth.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.auth.config.JwtSecurityProperties;
import com.joajy.spendingguard.auth.service.model.RefreshTokenOwner;
import com.joajy.spendingguard.auth.service.port.RefreshTokenStore;
import com.joajy.spendingguard.auth.service.result.RefreshToken;
import org.springframework.stereotype.Component;

@Component
class RefreshTokenPersistenceAdapter implements RefreshTokenStore {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenJpaRepository repository;
    private final JwtSecurityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository repository,
                                   JwtSecurityProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public RefreshToken create(UUID userId, Instant issuedAt) {
        String rawToken = newToken();
        Instant expiresAt = issuedAt.plus(properties.refreshTokenTtl());
        repository.save(new RefreshTokenEntity(
                UUID.randomUUID(), userId, hash(rawToken), issuedAt, expiresAt
        ));
        return new RefreshToken(rawToken, expiresAt);
    }

    @Override
    public Optional<RefreshTokenOwner> rotate(String token, Instant rotatedAt) {
        return repository.findByTokenHashForUpdate(hash(token))
                .filter(stored -> stored.isUsableAt(rotatedAt))
                .map(stored -> {
                    stored.revoke(rotatedAt);
                    var replacement = create(stored.getUserId(), rotatedAt);
                    return new RefreshTokenOwner(stored.getUserId(), replacement);
                });
    }

    @Override
    public void revoke(String token, Instant revokedAt) {
        repository.findByTokenHashForUpdate(hash(token))
                .filter(stored -> stored.isUsableAt(revokedAt))
                .ifPresent(stored -> stored.revoke(revokedAt));
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
