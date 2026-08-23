package com.joajy.spendingguard.auth.service.result;

import java.time.Instant;
import java.util.UUID;

public record AccessToken(
        String tokenType,
        String accessToken,
        Instant expiresAt,
        UUID userId
) {
}
