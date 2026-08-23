package com.joajy.spendingguard.auth.service.port;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.auth.service.result.AccessToken;

public interface IssueAccessTokenPort {
    AccessToken issue(UUID userId, String email, Instant issuedAt);
}
