package com.joajy.spendingguard.riskalert.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.riskalert.service.model.RiskAlertQuery;
import com.joajy.spendingguard.riskalert.service.port.RiskAlertQueryPort;
import com.joajy.spendingguard.riskalert.service.result.RiskAlertItem;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** 자동 분석과 사용자 수정 결과를 합쳐 유효 위험 알림을 조회한다. */
@Component
class RiskAlertPersistenceAdapter implements RiskAlertQueryPort {

    private final JdbcClient jdbcClient;

    RiskAlertPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean userExists(UUID userId) {
        return jdbcClient.sql("SELECT EXISTS(SELECT 1 FROM user_account WHERE id = :userId)")
                .param("userId", userId)
                .query(Boolean.class)
                .single();
    }

    @Override
    public List<RiskAlertItem> find(RiskAlertQuery query) {
        StringBuilder sql = new StringBuilder("""
                SELECT event.id AS event_id,
                       event.sanitized_message,
                       COALESCE(event.occurred_at, event.received_at) AS transaction_at,
                       parse.amount,
                       COALESCE(category_override.category, parse.category) AS category,
                       COALESCE(category_override.risk_level, parse.risk_level) AS risk_level,
                       COALESCE(category_override.risk_reason, parse.risk_reason) AS risk_reason,
                       COALESCE(category_override.version, 0) AS category_version
                  FROM raw_spend_event event
                  JOIN fast_parse_result parse ON parse.raw_event_id = event.id
                  LEFT JOIN spend_category_override category_override
                    ON category_override.spend_event_id = event.id
                 WHERE event.user_id = :userId
                   AND COALESCE(event.occurred_at, event.received_at) >= :from
                   AND COALESCE(event.occurred_at, event.received_at) < :until
                   AND parse.status = 'PARSED'
                   AND parse.transaction_type = 'PAYMENT'
                """);
        if (query.minimumLevel() == RiskLevel.HIGH) {
            sql.append(" AND COALESCE(category_override.risk_level, parse.risk_level) = 'HIGH'");
        } else {
            sql.append(" AND COALESCE(category_override.risk_level, parse.risk_level) IN ('HIGH', 'MEDIUM')");
        }
        if (query.cursorTransactionAt() != null) {
            sql.append("""
                     AND (COALESCE(event.occurred_at, event.received_at) < :cursorAt
                          OR (COALESCE(event.occurred_at, event.received_at) = :cursorAt
                              AND event.id < :cursorId))
                    """);
        }
        sql.append("""
                 ORDER BY COALESCE(event.occurred_at, event.received_at) DESC, event.id DESC
                 LIMIT :limit
                """);

        JdbcClient.StatementSpec statement = jdbcClient.sql(sql.toString())
                .param("userId", query.userId())
                .param("from", query.from().atOffset(ZoneOffset.UTC))
                .param("until", query.until().atOffset(ZoneOffset.UTC))
                .param("limit", query.limit());
        if (query.cursorTransactionAt() != null) {
            statement = statement
                    .param("cursorAt", query.cursorTransactionAt().atOffset(ZoneOffset.UTC))
                    .param("cursorId", query.cursorEventId());
        }
        return statement.query(this::map).list();
    }

    private RiskAlertItem map(ResultSet row, int rowNumber) throws SQLException {
        return new RiskAlertItem(
                row.getObject("event_id", UUID.class),
                row.getString("sanitized_message"),
                row.getObject("transaction_at", OffsetDateTime.class).toInstant(),
                row.getBigDecimal("amount"),
                row.getString("category"),
                RiskLevel.valueOf(row.getString("risk_level")),
                row.getString("risk_reason"),
                row.getLong("category_version")
        );
    }
}
