package com.joajy.spendingguard.auth.service.model;

import java.util.UUID;

public record LoginAccount(
        UUID userId,
        String email,
        String passwordHash,
        boolean emailVerified
) {
}
