package com.joajy.spendingguard.auth.repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.joajy.spendingguard.auth.service.LoginService;
import com.joajy.spendingguard.auth.service.TokenLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spending-guard.outbox.publisher.enabled=false",
        "spending-guard.analysis.consumer.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class RefreshTokenPersistenceIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("74f23c90-d96c-4428-92be-c793bd3ca902");
    private static final Instant NOW = Instant.parse("2026-08-24T01:00:00Z");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RefreshTokenPersistenceAdapter adapter;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private LoginService loginService;

    @Autowired
    private TokenLifecycleService tokenLifecycleService;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM refresh_token").update();
        jdbcClient.sql("DELETE FROM user_account").update();
        jdbcClient.sql("""
                INSERT INTO user_account
                    (id, email, password_hash, created_at, email_verified_at)
                VALUES (:id, 'token@example.com', :passwordHash, :now, :now)
                """)
                .param("id", USER_ID)
                .param("passwordHash", new BCryptPasswordEncoder().encode("password-123"))
                .param("now", NOW.atOffset(ZoneOffset.UTC))
                .update();
    }

    @Test
    void storesOnlyHashAndRejectsRefreshTokenReuse() {
        var issued = adapter.create(USER_ID, NOW);

        String storedHash = jdbcClient.sql("SELECT token_hash FROM refresh_token")
                .query(String.class).single();
        assertThat(storedHash).hasSize(64).isNotEqualTo(issued.value());

        var rotated = inTransaction(() -> adapter.rotate(issued.value(), NOW.plusSeconds(1)));
        var reused = inTransaction(() -> adapter.rotate(issued.value(), NOW.plusSeconds(2)));

        assertThat(rotated).isPresent();
        assertThat(rotated.orElseThrow().replacement().value()).isNotEqualTo(issued.value());
        assertThat(reused).isEmpty();
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM refresh_token")
                .query(Long.class).single()).isEqualTo(2);
    }

    @Test
    void persistsRefreshTokenDuringLoginForImmediateRotation() {
        var login = loginService.login("token@example.com", "password-123");

        var refreshed = tokenLifecycleService.refresh(login.refreshToken());

        assertThat(refreshed.userId()).isEqualTo(USER_ID);
        assertThat(refreshed.refreshToken()).isNotEqualTo(login.refreshToken());
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM refresh_token")
                .query(Long.class).single()).isEqualTo(2);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM refresh_token WHERE revoked_at IS NOT NULL")
                .query(Long.class).single()).isOne();
    }

    private <T> T inTransaction(java.util.function.Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }
}
