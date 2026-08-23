package com.joajy.spendingguard.account.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.joajy.spendingguard.account.service.command.RegisterUserCommand;
import com.joajy.spendingguard.account.service.exception.DuplicateEmailException;
import com.joajy.spendingguard.account.service.port.outbound.StoreUserAccountPort;
import com.joajy.spendingguard.account.domain.model.UserAccount;
import com.joajy.spendingguard.account.repository.UserAccountJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spending-guard.outbox.publisher.enabled=false",
        "spending-guard.analysis.consumer.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class UserRegistrationPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserRegistrationService registrationService;

    @Autowired
    private StoreUserAccountPort storeUserAccountPort;

    @Autowired
    private UserAccountJpaRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void storesBcryptHashInsteadOfRawPassword() {
        String rawPassword = "safe-password-123";
        var registration = registrationService.register(new RegisterUserCommand(
                "  User@Example.COM ",
                rawPassword
        ));

        var stored = repository.findById(registration.userId()).orElseThrow();
        assertThat(stored.getEmail()).isEqualTo("user@example.com");
        assertThat(stored.getPasswordHash()).isNotEqualTo(rawPassword).startsWith("$2a$12$");
        assertThat(new BCryptPasswordEncoder().matches(rawPassword, stored.getPasswordHash())).isTrue();
    }

    @Test
    void returnsConflictMeaningForNormalizedDuplicateEmail() {
        registrationService.register(new RegisterUserCommand("user@example.com", "safe-password-123"));

        assertThatThrownBy(() -> registrationService.register(new RegisterUserCommand(
                " USER@EXAMPLE.COM ",
                "another-password-123"
        ))).isInstanceOf(DuplicateEmailException.class);
        assertThat(repository.count()).isOne();
    }

    @Test
    void acceptsOnlyOneConcurrentInsertForSameEmail() throws Exception {
        int attempts = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);

        try {
            List<Future<Result>> futures = java.util.stream.IntStream.range(0, attempts)
                    .mapToObj(index -> executor.submit((Callable<Result>) () -> storeAfter(start, index)))
                    .toList();
            start.countDown();

            List<Result> results = futures.stream().map(this::get).toList();
            assertThat(results).containsOnlyOnce(Result.CREATED);
            assertThat(results).filteredOn(Result.DUPLICATE::equals).hasSize(attempts - 1);
            assertThat(repository.count()).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    private Result storeAfter(CountDownLatch start, int index) throws InterruptedException {
        start.await();
        try {
            storeUserAccountPort.store(new UserAccount(
                    UUID.randomUUID(),
                    "concurrent@example.com",
                    "test-hash-" + index,
                    Instant.parse("2026-08-20T01:00:00Z")
            ));
            return Result.CREATED;
        } catch (DuplicateEmailException exception) {
            return Result.DUPLICATE;
        }
    }

    private Result get(Future<Result> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError("동시 회원 등록 결과를 확인할 수 없습니다.", exception);
        }
    }

    private enum Result {
        CREATED,
        DUPLICATE
    }
}
