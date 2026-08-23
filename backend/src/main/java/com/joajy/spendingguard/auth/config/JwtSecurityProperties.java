package com.joajy.spendingguard.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spending-guard.security")
public record JwtSecurityProperties(String issuer, String secret, Duration accessTokenTtl) {
    public JwtSecurityProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT issuer is required");
        }
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalArgumentException("JWT access token TTL must be positive");
        }
    }
}
