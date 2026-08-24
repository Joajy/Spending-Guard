package com.joajy.spendingguard.auth.service.model;

import java.util.UUID;

import com.joajy.spendingguard.auth.service.result.RefreshToken;

public record RefreshTokenOwner(UUID userId, RefreshToken replacement) {
}
