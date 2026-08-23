package com.joajy.spendingguard.auth.security;

import java.util.UUID;
import java.time.Instant;

import com.joajy.spendingguard.auth.config.JwtSecurityProperties;
import com.joajy.spendingguard.auth.service.port.IssueAccessTokenPort;
import com.joajy.spendingguard.auth.service.result.AccessToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

@Component
class JwtAccessTokenIssuer implements IssueAccessTokenPort {

    private final JwtEncoder encoder;
    private final JwtSecurityProperties properties;

    JwtAccessTokenIssuer(JwtEncoder encoder, JwtSecurityProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public AccessToken issue(UUID userId, String email, Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        var claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("email", email)
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken("Bearer", token, expiresAt, userId);
    }
}
