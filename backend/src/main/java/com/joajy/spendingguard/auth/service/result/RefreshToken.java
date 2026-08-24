package com.joajy.spendingguard.auth.service.result;

import java.time.Instant;

public record RefreshToken(String value, Instant expiresAt) {
}
