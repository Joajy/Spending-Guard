package com.joajy.spendingguard.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.account.domain.policy.EmailNormalizer;
import com.joajy.spendingguard.account.service.port.outbound.VerifyPasswordPort;
import com.joajy.spendingguard.auth.service.exception.InvalidCredentialsException;
import com.joajy.spendingguard.auth.service.exception.UnverifiedEmailException;
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

class LoginServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-23T01:00:00Z");
    private final LoadLoginAccountPort accounts = mock(LoadLoginAccountPort.class);
    private final VerifyPasswordPort passwords = mock(VerifyPasswordPort.class);
    private final IssueAccessTokenPort tokens = mock(IssueAccessTokenPort.class);
    private final RefreshTokenStore refreshTokens = mock(RefreshTokenStore.class);
    private final LoginService service = new LoginService(
            accounts,
            passwords,
            tokens,
            refreshTokens,
            new EmailNormalizer(),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void issuesTokenForVerifiedAccountAndCorrectPassword() {
        var account = new LoginAccount(USER_ID, "user@example.com", "hash", true);
        var expected = new AccessToken("Bearer", "token", NOW.plusSeconds(900), USER_ID);
        var expectedRefresh = new RefreshToken("refresh-token", NOW.plusSeconds(1209600));
        given(accounts.findByEmail("user@example.com")).willReturn(Optional.of(account));
        given(passwords.matches("password-123", "hash")).willReturn(true);
        given(tokens.issue(USER_ID, "user@example.com", NOW)).willReturn(expected);
        given(refreshTokens.create(USER_ID, NOW)).willReturn(expectedRefresh);

        var result = service.login(" User@Example.com ", "password-123");

        assertThat(result).isEqualTo(AuthTokens.from(expected, expectedRefresh));
    }

    @Test
    void usesSameErrorForUnknownEmailAndWrongPassword() {
        given(accounts.findByEmail("user@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("user@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email or password is incorrect");
    }

    @Test
    void rejectsLoginBeforeEmailVerification() {
        var account = new LoginAccount(USER_ID, "user@example.com", "hash", false);
        given(accounts.findByEmail("user@example.com")).willReturn(Optional.of(account));
        given(passwords.matches("password-123", "hash")).willReturn(true);

        assertThatThrownBy(() -> service.login("user@example.com", "password-123"))
                .isInstanceOf(UnverifiedEmailException.class);
    }
}
