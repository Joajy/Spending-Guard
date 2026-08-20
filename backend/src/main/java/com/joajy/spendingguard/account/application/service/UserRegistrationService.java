package com.joajy.spendingguard.account.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.account.application.command.RegisterUserCommand;
import com.joajy.spendingguard.account.application.port.inbound.RegisterUserUseCase;
import com.joajy.spendingguard.account.application.port.outbound.HashPasswordPort;
import com.joajy.spendingguard.account.application.port.outbound.StoreUserAccountPort;
import com.joajy.spendingguard.account.application.result.UserRegistration;
import com.joajy.spendingguard.account.domain.model.UserAccount;
import com.joajy.spendingguard.account.domain.policy.EmailNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일을 정규화하고 평문 비밀번호를 해시한 뒤 새 계정을 저장하는 유스케이스다.
 *
 * <p>중복 여부를 먼저 조회하지 않는다. 동시에 같은 이메일이 들어와도 데이터베이스 고유
 * 제약이 한 요청만 허용하며, 영속성 어댑터가 위반을 업무 예외로 변환한다.
 */
@Service
public class UserRegistrationService implements RegisterUserUseCase {

    private final StoreUserAccountPort storeUserAccountPort;
    private final HashPasswordPort hashPasswordPort;
    private final EmailNormalizer emailNormalizer;
    private final Clock clock;

    public UserRegistrationService(
            StoreUserAccountPort storeUserAccountPort,
            HashPasswordPort hashPasswordPort,
            EmailNormalizer emailNormalizer,
            Clock clock
    ) {
        this.storeUserAccountPort = storeUserAccountPort;
        this.hashPasswordPort = hashPasswordPort;
        this.emailNormalizer = emailNormalizer;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UserRegistration register(RegisterUserCommand command) {
        UUID userId = UUID.randomUUID();
        Instant createdAt = clock.instant();
        String normalizedEmail = emailNormalizer.normalize(command.email());
        UserAccount account = new UserAccount(
                userId,
                normalizedEmail,
                hashPasswordPort.hash(command.password()),
                createdAt
        );

        storeUserAccountPort.store(account);
        return new UserRegistration(userId, normalizedEmail, createdAt);
    }
}
