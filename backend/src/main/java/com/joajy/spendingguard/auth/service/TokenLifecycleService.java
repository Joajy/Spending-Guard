package com.joajy.spendingguard.auth.service;

import java.time.Clock;

import com.joajy.spendingguard.auth.service.exception.InvalidRefreshTokenException;
import com.joajy.spendingguard.auth.service.port.IssueAccessTokenPort;
import com.joajy.spendingguard.auth.service.port.LoadLoginAccountPort;
import com.joajy.spendingguard.auth.service.port.RefreshTokenStore;
import com.joajy.spendingguard.auth.service.result.AuthTokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenLifecycleService {

    private final RefreshTokenStore refreshTokenStore;
    private final IssueAccessTokenPort issueAccessTokenPort;
    private final LoadLoginAccountPort loadLoginAccountPort;
    private final Clock clock;

    public TokenLifecycleService(
            RefreshTokenStore refreshTokenStore,
            IssueAccessTokenPort issueAccessTokenPort,
            LoadLoginAccountPort loadLoginAccountPort,
            Clock clock
    ) {
        this.refreshTokenStore = refreshTokenStore;
        this.issueAccessTokenPort = issueAccessTokenPort;
        this.loadLoginAccountPort = loadLoginAccountPort;
        this.clock = clock;
    }

    @Transactional
    public AuthTokens refresh(String token) {
        var now = clock.instant();
        var owner = refreshTokenStore.rotate(token, now)
                .orElseThrow(InvalidRefreshTokenException::new);
        var account = loadLoginAccountPort.findById(owner.userId())
                .orElseThrow(InvalidRefreshTokenException::new);
        var accessToken = issueAccessTokenPort.issue(account.userId(), account.email(), now);
        return AuthTokens.from(accessToken, owner.replacement());
    }

    @Transactional
    public void logout(String token) {
        refreshTokenStore.revoke(token, clock.instant());
    }
}
