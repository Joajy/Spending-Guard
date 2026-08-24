package com.joajy.spendingguard.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.joajy.spendingguard.auth.config.JwtSecurityProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAccessTokenIssuerTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-bytes";
    private static final JwtSecurityProperties PROPERTIES = new JwtSecurityProperties(
            "https://spending-guard.local", SECRET, Duration.ofMinutes(15), Duration.ofDays(14)
    );
    private final SecretKey key = new SecretKeySpec(
            SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
    );
    private final JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(
            new NimbusJwtEncoder(new ImmutableSecret<>(key)), PROPERTIES
    );

    @Test
    void signsSubjectEmailIssuerAndExpiry() {
        UUID userId = UUID.randomUUID();
        Instant issuedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        var token = issuer.issue(userId, "user@example.com", issuedAt);
        var decoded = decoder().decode(token.accessToken());

        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getIssuer().toString()).isEqualTo("https://spending-guard.local");
        assertThat(decoded.getClaimAsString("email")).isEqualTo("user@example.com");
        assertThat(decoded.getExpiresAt()).isEqualTo(issuedAt.plusSeconds(900));
    }

    @Test
    void rejectsExpiredToken() {
        var token = issuer.issue(
                UUID.randomUUID(), "user@example.com", Instant.now().minus(Duration.ofHours(1))
        );

        assertThatThrownBy(() -> decoder().decode(token.accessToken()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedSignature() {
        var token = issuer.issue(UUID.randomUUID(), "user@example.com", Instant.now());
        String tampered = token.accessToken().substring(0, token.accessToken().length() - 1) + "x";

        assertThatThrownBy(() -> decoder().decode(tampered)).isInstanceOf(JwtException.class);
    }

    private NimbusJwtDecoder decoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(PROPERTIES.issuer()));
        return decoder;
    }
}
