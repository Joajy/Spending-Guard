package com.joajy.spendingguard.auth.service;

import java.time.Clock;

import com.joajy.spendingguard.account.domain.policy.EmailNormalizer;
import com.joajy.spendingguard.account.service.port.outbound.VerifyPasswordPort;
import com.joajy.spendingguard.auth.service.exception.InvalidCredentialsException;
import com.joajy.spendingguard.auth.service.exception.UnverifiedEmailException;
import com.joajy.spendingguard.auth.service.port.IssueAccessTokenPort;
import com.joajy.spendingguard.auth.service.port.LoadLoginAccountPort;
import com.joajy.spendingguard.auth.service.port.RefreshTokenStore;
import com.joajy.spendingguard.auth.service.result.AuthTokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 자격 증명을 확인하고 이메일 인증이 끝난 사용자에게 짧은 수명의 토큰을 발급한다. */
@Service
public class LoginService {

    private final LoadLoginAccountPort loadLoginAccountPort;
    private final VerifyPasswordPort verifyPasswordPort;
    private final IssueAccessTokenPort issueAccessTokenPort;
    private final RefreshTokenStore refreshTokenStore;
    private final EmailNormalizer emailNormalizer;
    private final Clock clock;

    public LoginService(
            LoadLoginAccountPort loadLoginAccountPort,
            VerifyPasswordPort verifyPasswordPort,
            IssueAccessTokenPort issueAccessTokenPort,
            RefreshTokenStore refreshTokenStore,
            EmailNormalizer emailNormalizer,
            Clock clock
    ) {
        this.loadLoginAccountPort = loadLoginAccountPort;
        this.verifyPasswordPort = verifyPasswordPort;
        this.issueAccessTokenPort = issueAccessTokenPort;
        this.refreshTokenStore = refreshTokenStore;
        this.emailNormalizer = emailNormalizer;
        this.clock = clock;
    }

    @Transactional
    public AuthTokens login(String email, String password) {
        var account = loadLoginAccountPort.findByEmail(emailNormalizer.normalize(email))
                .orElseThrow(InvalidCredentialsException::new);
        if (!verifyPasswordPort.matches(password, account.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!account.emailVerified()) {
            throw new UnverifiedEmailException();
        }
        var issuedAt = clock.instant();
        var accessToken = issueAccessTokenPort.issue(account.userId(), account.email(), issuedAt);
        var refreshToken = refreshTokenStore.create(account.userId(), issuedAt);
        return AuthTokens.from(accessToken, refreshToken);
    }
}
