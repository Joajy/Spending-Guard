package com.joajy.spendingguard.account.application.service;

import java.security.SecureRandom;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import com.joajy.spendingguard.account.application.exception.InvalidVerificationCodeException;
import com.joajy.spendingguard.account.application.exception.UserAccountNotFoundException;
import com.joajy.spendingguard.account.application.port.outbound.EmailVerificationStore;
import com.joajy.spendingguard.account.application.port.outbound.SendVerificationCodePort;
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

    @Test void confirmsValidUnexpiredCodeOnce() {
        given(store.findChallenge(USER_ID)).willReturn(Optional.of(new EmailVerificationStore.Challenge("hash", NOW.plusSeconds(1))));
        given(encoder.matches("123456", "hash")).willReturn(true);
        given(store.markVerifiedAndDeleteChallenge(USER_ID, NOW)).willReturn(true);
        service.confirm(USER_ID, "123456");
        then(store).should().markVerifiedAndDeleteChallenge(USER_ID, NOW);
    }

    @Test void rejectsExpiredCodeWithoutChangingAccount() {
        given(store.findChallenge(USER_ID)).willReturn(Optional.of(new EmailVerificationStore.Challenge("hash", NOW)));
        assertThatThrownBy(() -> service.confirm(USER_ID, "123456")).isInstanceOf(InvalidVerificationCodeException.class);
        then(store).should(never()).markVerifiedAndDeleteChallenge(any(), any());
    }

    @Test void rejectsIncorrectCodeWithoutChangingAccount() {
        given(store.findChallenge(USER_ID)).willReturn(Optional.of(new EmailVerificationStore.Challenge("hash", NOW.plusSeconds(1))));
        given(encoder.matches("654321", "hash")).willReturn(false);
        assertThatThrownBy(() -> service.confirm(USER_ID, "654321")).isInstanceOf(InvalidVerificationCodeException.class);
        then(store).should(never()).markVerifiedAndDeleteChallenge(any(), any());
    }
}
