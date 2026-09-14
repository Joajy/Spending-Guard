package com.joajy.spendingguard.spendevent.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class SpendEventDeduplicationMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void migratesExistingRowsWithoutWeakeningDuplicateProtection() throws SQLException {
        migrateToVersion16();

        UUID firstUser = UUID.randomUUID();
        UUID secondUser = UUID.randomUUID();
        insertUser(firstUser, "first@example.com");
        insertUser(secondUser, "second@example.com");
        insertEvent(UUID.randomUUID(), firstUser, "shared-event", "shared-key");
        insertEvent(UUID.randomUUID(), null, "legacy-event", "legacy-key");

        migrateToLatest();

        insertEvent(UUID.randomUUID(), secondUser, "shared-event", "shared-key");

        assertThatThrownBy(() -> insertEvent(UUID.randomUUID(), firstUser, "shared-event", "shared-key"))
                .isInstanceOf(SQLException.class)
                .hasFieldOrPropertyWithValue("SQLState", "23505");
        assertThatThrownBy(() -> insertEvent(UUID.randomUUID(), null, "legacy-event", "legacy-key"))
                .isInstanceOf(SQLException.class)
                .hasFieldOrPropertyWithValue("SQLState", "23505");
        assertThat(countSpendEvents()).isEqualTo(3);
    }

    private void migrateToVersion16() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target(MigrationVersion.fromVersion("16"))
                .load()
                .migrate();
    }

    private void migrateToLatest() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();
    }

    private void insertUser(UUID userId, String email) throws SQLException {
        String sql = """
                INSERT INTO user_account (id, email, password_hash, created_at)
                VALUES (?, ?, 'test-password-hash', now())
                """;
        try (Connection connection = POSTGRES.createConnection("");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            statement.setString(2, email);
            statement.executeUpdate();
        }
    }

    private void insertEvent(UUID eventId, UUID userId, String externalEventId, String deduplicationKey)
            throws SQLException {
        String sql = """
                INSERT INTO raw_spend_event (
                    id, user_id, source, external_event_id, deduplication_key,
                    sanitized_message, status, occurred_at, received_at
                ) VALUES (?, ?, 'SIMULATOR', ?, ?, '테스트 결제', 'RECEIVED', now(), now())
                """;
        try (Connection connection = POSTGRES.createConnection("");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, eventId);
            statement.setObject(2, userId, Types.OTHER);
            statement.setString(3, externalEventId);
            statement.setString(4, deduplicationKey);
            statement.executeUpdate();
        }
    }

    private long countSpendEvents() throws SQLException {
        try (Connection connection = POSTGRES.createConnection("");
             PreparedStatement statement = connection.prepareStatement("SELECT count(*) FROM raw_spend_event");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
