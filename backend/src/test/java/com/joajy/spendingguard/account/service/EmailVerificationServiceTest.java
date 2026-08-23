package com.joajy.spendingguard.account.service;

import java.security.SecureRandom;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import com.joajy.spendingguard.account.service.exception.InvalidVerificationCodeException;
import com.joajy.spendingguard.account.service.exception.TooManyVerificationAttemptsException;
import com.joajy.spendingguard.account.service.exception.UserAccountNotFoundException;
import com.joajy.spendingguard.account.service.exception.VerificationCodeRequestTooFrequentException;
import com.joajy.spendingguard.account.service.port.outbound.EmailVerificationStore;
import com.joajy.spendingguard.account.service.port.outbound.SendVerificationCodePort;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

class EmailVerificationServiceTest {
    private static final UUID USER_ID = UUID.fromString("9bbd364c-a952-42aa-91cb-f607603aa7d5");
    private static final Instant NOW = Instant.parse("2026-08-23T01:00:00Z");
    private final EmailVerificationStore store = mock(EmailVerificationStore.class);
    private final SendVerificationCodePort sender = mock(SendVerificationCodePort.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final SecureRandom random = mock(SecureRandom.class);
    private final EmailVerificationService service = new EmailVerificationService(store, sender, encoder, random,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test void issuesSixDigitCodeAndStoresOnlyHash() {
        given(store.findAccount(USER_ID)).willReturn(Optional.of(new EmailVerificationStore.AccountTarget(USER_ID, "user@example.com", false)));
        given(store.findChallenge(USER_ID)).willReturn(Optional.empty());
        given(random.nextInt(1_000_000)).willReturn(42);
        given(encoder.encode("000042")).willReturn("hashed-code");
        service.issue(USER_ID);
        then(store).should().replaceChallenge(USER_ID, "hashed-code", NOW.plus(Duration.ofMinutes(5)), NOW);
        then(sender).should().send("user@example.com", "000042");
    }

    @Test void doesNotIssueAnotherCodeForVerifiedAccount() {
        given(store.findAccount(USER_ID)).willReturn(Optional.of(new EmailVerificationStore.AccountTarget(USER_ID, "user@example.com", true)));
        service.issue(USER_ID);
        then(sender).shouldHaveNoInteractions();
        then(store).should(never()).replaceChallenge(any(), any(), any(), any());
    }

    @Test void rejectsUnknownAccount() {
        given(store.findAccount(USER_ID)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.issue(USER_ID)).isInstanceOf(UserAccountNotFoundException.class);
    }

    @Test void rejectsResendWithinOneMinute() {
        given(store.findAccount(USER_ID)).willReturn(Optional.of(new EmailVerificationStore.AccountTarget(USER_ID, "user@example.com", false)));
        given(store.findChallenge(USER_ID)).willReturn(Optional.of(challenge(NOW.plus(Duration.ofMinutes(5)), NOW.minusSeconds(59), 0)));

        assertThatThrownBy(() -> service.issue(USER_ID)).isInstanceOf(VerificationCodeRequestTooFrequentException.class);
        then(sender).shouldHaveNoInteractions();
    }

    @Test void confirmsValidUnexpiredCodeOnce() {
        givenAccount();
        given(store.findChallenge(USER_ID)).willReturn(Optional.of(challenge(NOW.plusSeconds(1), NOW.minusSeconds(1), 0)));
        given(encoder.matches("123456", "hash")).willReturn(true);
        given(store.markVerifiedAndDeleteChallenge(USER_ID, NOW)).willReturn(true);
        service.confirm(USER_ID, "123456");
        then(store).should().markVerifiedAndDeleteChallenge(USER_ID, NOW);
    }

    @Test void rejectsExpiredCodeWithoutChangingAccount() {
        givenAccount();
        given(store.findChallenge(USER_ID)).willReturn(Optional.of(challenge(NOW, NOW.minusSeconds(1), 0)));
        assertThatThrownBy(() -> service.confirm(USER_ID, "123456")).isInstanceOf(InvalidVerificationCodeException.class);
        then(store).should(never()).markVerifiedAndDeleteChallenge(any(), any());
    }

    @Test void rejectsIncorrectCodeWithoutChangingAccount() {
        givenAccount();
        given(store.findChallenge(USER_ID)).willReturn(Optional.of(challenge(NOW.plusSeconds(1), NOW.minusSeconds(1), 0)));
        given(encoder.matches("654321", "hash")).willReturn(false);
        given(store.incrementFailedAttempts(USER_ID)).willReturn(1);
        assertThatThrownBy(() -> service.confirm(USER_ID, "654321")).isInstanceOf(InvalidVerificationCodeException.class);
        then(store).should(never()).markVerifiedAndDeleteChallenge(any(), any());
    }

    @Test void locksChallengeOnFifthIncorrectCode() {
        givenAccount();
        given(store.findChallenge(USER_ID)).willReturn(Optional.of(challenge(NOW.plusSeconds(1), NOW.minusSeconds(1), 4)));
        given(encoder.matches("654321", "hash")).willReturn(false);
        given(store.incrementFailedAttempts(USER_ID)).willReturn(5);

        assertThatThrownBy(() -> service.confirm(USER_ID, "654321"))
                .isInstanceOf(TooManyVerificationAttemptsException.class);
    }

    private void givenAccount() {
        given(store.findAccount(USER_ID)).willReturn(Optional.of(new EmailVerificationStore.AccountTarget(USER_ID, "user@example.com", false)));
    }

    private EmailVerificationStore.Challenge challenge(Instant expiresAt, Instant createdAt, int failedAttempts) {
        return new EmailVerificationStore.Challenge("hash", expiresAt, createdAt, failedAttempts);
    }
}
