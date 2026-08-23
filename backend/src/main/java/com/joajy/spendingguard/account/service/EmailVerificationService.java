package com.joajy.spendingguard.account.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.account.service.exception.InvalidVerificationCodeException;
import com.joajy.spendingguard.account.service.exception.TooManyVerificationAttemptsException;
import com.joajy.spendingguard.account.service.exception.UserAccountNotFoundException;
import com.joajy.spendingguard.account.service.exception.VerificationCodeRequestTooFrequentException;
import com.joajy.spendingguard.account.service.port.inbound.VerifyEmailUseCase;
import com.joajy.spendingguard.account.service.port.outbound.EmailVerificationStore;
import com.joajy.spendingguard.account.service.port.outbound.SendVerificationCodePort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 인증번호의 발급, 만료, 일회성 사용 규칙을 한 트랜잭션 경계에서 처리한다. */
@Service
public class EmailVerificationService implements VerifyEmailUseCase {
    private static final Duration VALIDITY = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final EmailVerificationStore store;
    private final SendVerificationCodePort sender;
    private final PasswordEncoder encoder;
    private final SecureRandom random;
    private final Clock clock;

    public EmailVerificationService(EmailVerificationStore store, SendVerificationCodePort sender,
                                    PasswordEncoder encoder, SecureRandom random, Clock clock) {
        this.store = store;
        this.sender = sender;
        this.encoder = encoder;
        this.random = random;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void issue(UUID userId) {
        var account = store.findAccount(userId).orElseThrow(UserAccountNotFoundException::new);
        if (account.verified()) {
            return;
        }
        Instant now = clock.instant();
        store.findChallenge(userId).ifPresent(challenge -> {
            if (now.isBefore(challenge.createdAt().plus(RESEND_COOLDOWN))) {
                throw new VerificationCodeRequestTooFrequentException();
            }
        });
        String code = "%06d".formatted(random.nextInt(1_000_000));
        store.replaceChallenge(userId, encoder.encode(code), now.plus(VALIDITY), now);
        sender.send(account.email(), code);
    }

    @Override
    @Transactional
    public void confirm(UUID userId, String code) {
        store.findAccount(userId).orElseThrow(UserAccountNotFoundException::new);
        var challenge = store.findChallenge(userId).orElseThrow(InvalidVerificationCodeException::new);
        if (!clock.instant().isBefore(challenge.expiresAt())) {
            throw new InvalidVerificationCodeException();
        }
        if (challenge.failedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new TooManyVerificationAttemptsException();
        }
        if (!encoder.matches(code, challenge.codeHash())) {
            if (store.incrementFailedAttempts(userId) >= MAX_FAILED_ATTEMPTS) {
                throw new TooManyVerificationAttemptsException();
            }
            throw new InvalidVerificationCodeException();
        }
        if (!store.markVerifiedAndDeleteChallenge(userId, clock.instant())) {
            throw new UserAccountNotFoundException();
        }
    }
}
