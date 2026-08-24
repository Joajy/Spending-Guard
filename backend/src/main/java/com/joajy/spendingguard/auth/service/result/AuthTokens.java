package com.joajy.spendingguard.auth.service.result;

import java.time.Instant;
import java.util.UUID;

public record AuthTokens(
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UUID userId
) {
    public static AuthTokens from(AccessToken accessToken, RefreshToken refreshToken) {
        return new AuthTokens(
                accessToken.tokenType(),
                accessToken.accessToken(),
                accessToken.expiresAt(),
                refreshToken.value(),
                refreshToken.expiresAt(),
                accessToken.userId()
        );
    }
}
