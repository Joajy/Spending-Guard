package com.joajy.spendingguard.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.auth.service.exception.InvalidRefreshTokenException;
import com.joajy.spendingguard.auth.service.model.RefreshTokenOwner;
import com.joajy.spendingguard.auth.service.model.LoginAccount;
import com.joajy.spendingguard.auth.service.port.IssueAccessTokenPort;
import com.joajy.spendingguard.auth.service.port.LoadLoginAccountPort;
import com.joajy.spendingguard.auth.service.port.RefreshTokenStore;
import com.joajy.spendingguard.auth.service.result.AccessToken;
import com.joajy.spendingguard.auth.service.result.AuthTokens;
import com.joajy.spendingguard.auth.service.result.RefreshToken;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TokenLifecycleServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-24T01:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private final RefreshTokenStore store = mock(RefreshTokenStore.class);
    private final IssueAccessTokenPort accessTokens = mock(IssueAccessTokenPort.class);
    private final LoadLoginAccountPort accounts = mock(LoadLoginAccountPort.class);
    private final TokenLifecycleService service = new TokenLifecycleService(
            store, accessTokens, accounts, Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void rotatesRefreshTokenAndIssuesNewAccessToken() {
        var replacement = new RefreshToken("replacement", NOW.plusSeconds(1209600));
        var access = new AccessToken("Bearer", "access", NOW.plusSeconds(900), USER_ID);
        given(store.rotate("current", NOW)).willReturn(Optional.of(
                new RefreshTokenOwner(USER_ID, replacement)
        ));
        given(accounts.findById(USER_ID)).willReturn(Optional.of(
                new LoginAccount(USER_ID, "user@example.com", "hash", true)
        ));
        given(accessTokens.issue(USER_ID, "user@example.com", NOW)).willReturn(access);

        var result = service.refresh("current");

        assertThat(result).isEqualTo(AuthTokens.from(access, replacement));
    }

    @Test
    void rejectsExpiredOrReusedRefreshToken() {
        given(store.rotate("invalid", NOW)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("invalid"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokesRefreshTokenOnLogout() {
        service.logout("current");
        verify(store).revoke("current", NOW);
    }
}
