package com.joajy.spendingguard.account.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.joajy.spendingguard.account.application.command.RegisterUserCommand;
import com.joajy.spendingguard.account.application.port.outbound.HashPasswordPort;
import com.joajy.spendingguard.account.application.port.outbound.StoreUserAccountPort;
import com.joajy.spendingguard.account.domain.model.UserAccount;
import com.joajy.spendingguard.account.domain.policy.EmailNormalizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRegistrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");

    private final CapturingStore store = new CapturingStore();
    private final CapturingHasher hasher = new CapturingHasher();
    private final UserRegistrationService service = new UserRegistrationService(
            store,
            hasher,
            new EmailNormalizer(),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void normalizesEmailAndStoresOnlyPasswordHash() {
        var result = service.register(new RegisterUserCommand(
                "  User@Example.COM ",
                "correct-horse-battery-staple"
        ));

        assertThat(result.email()).isEqualTo("user@example.com");
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(hasher.rawPassword).isEqualTo("correct-horse-battery-staple");
        assertThat(store.account.email()).isEqualTo("user@example.com");
        assertThat(store.account.passwordHash()).isEqualTo("bcrypt-hash");
        assertThat(store.account.passwordHash()).doesNotContain("correct-horse");
    }

    private static final class CapturingStore implements StoreUserAccountPort {

        private UserAccount account;

        @Override
        public void store(UserAccount account) {
            this.account = account;
        }
    }

    private static final class CapturingHasher implements HashPasswordPort {

        private String rawPassword;

        @Override
        public String hash(String rawPassword) {
            this.rawPassword = rawPassword;
            return "bcrypt-hash";
        }
    }
}
