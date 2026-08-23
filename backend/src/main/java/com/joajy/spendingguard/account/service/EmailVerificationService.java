package com.joajy.spendingguard.account.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.account.service.exception.InvalidVerificationCodeException;
import com.joajy.spendingguard.account.service.exception.UserAccountNotFoundException;
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
        String code = "%06d".formatted(random.nextInt(1_000_000));
        Instant now = clock.instant();
        store.replaceChallenge(userId, encoder.encode(code), now.plus(VALIDITY), now);
        sender.send(account.email(), code);
    }

    @Override
    @Transactional
    public void confirm(UUID userId, String code) {
        var challenge = store.findChallenge(userId).orElseThrow(InvalidVerificationCodeException::new);
        if (!clock.instant().isBefore(challenge.expiresAt()) || !encoder.matches(code, challenge.codeHash())) {
            throw new InvalidVerificationCodeException();
        }
        if (!store.markVerifiedAndDeleteChallenge(userId, clock.instant())) {
            throw new UserAccountNotFoundException();
        }
    }
}
