package com.joajy.spendingguard.spendevent.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import com.joajy.spendingguard.spendevent.service.model.SpendEventHistoryQuery;
import com.joajy.spendingguard.spendevent.service.port.outbound.LoadSpendEventDetailPort;
import com.joajy.spendingguard.spendevent.service.port.outbound.LoadSpendEventHistoryPort;
import com.joajy.spendingguard.spendevent.service.result.FastParseResult;
import com.joajy.spendingguard.spendevent.service.result.SpendEventDetail;
import com.joajy.spendingguard.spendevent.service.result.SpendEventHistoryItem;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * 소비 이벤트 상세와 월별 목록을 읽는 조회 전용 영속성 어댑터다.
 *
 * <p>쓰기 엔티티 사이에 조회 편의를 위한 연관관계를 추가하지 않고 명시적인 LEFT JOIN을
 * 사용한다. 따라서 분석 전의 이벤트도 반환하며, API 요청 한 번에 추가 조회가 발생하지 않는다.
 * 목록은 거래 시각과 UUID의 복합 키를 내림차순으로 조회해 offset 증가에 따른 성능 저하와
 * 동률 시각에서의 중복·누락을 피한다.
 */
@Component
class SpendEventQueryPersistenceAdapter implements LoadSpendEventDetailPort, LoadSpendEventHistoryPort {

    private static final String FIND_BY_USER_AND_ID = """
            SELECT raw_event.id AS event_id,
                   raw_event.source,
                   raw_event.status AS event_status,
                   raw_event.occurred_at,
                   raw_event.received_at,
                   parse.amount,
                   parse.transaction_type,
                   parse.status AS parse_status,
                   parse.review_reason,
                   parse.category,
                   parse.fixed_cost,
                   parse.risk_level,
                   parse.risk_reason,
                   parse.parser_version,
                   parse.parsed_at
              FROM raw_spend_event raw_event
              LEFT JOIN fast_parse_result parse ON parse.raw_event_id = raw_event.id
             WHERE raw_event.user_id = :userId
               AND raw_event.id = :eventId
            """;

    private final JdbcClient jdbcClient;

    SpendEventQueryPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<SpendEventDetail> findByUserIdAndId(UUID userId, UUID eventId) {
        return jdbcClient.sql(FIND_BY_USER_AND_ID)
                .param("userId", userId)
                .param("eventId", eventId)
                .query(this::map)
                .optional();
    }

    @Override
    public Optional<SpendEventDetail> findById(UUID eventId) {
        String query = FIND_BY_USER_AND_ID.replaceAll(
                "raw_event\\.user_id = :userId\\s+AND\\s+",
                ""
        );
        return jdbcClient.sql(query)
                .param("eventId", eventId)
                .query(this::map)
                .optional();
    }

    @Override
    public List<SpendEventHistoryItem> find(SpendEventHistoryQuery query) {
        StringBuilder sql = new StringBuilder("""
                SELECT raw_event.id AS event_id,
                       raw_event.sanitized_message,
                       raw_event.status AS event_status,
                       COALESCE(raw_event.occurred_at, raw_event.received_at) AS transaction_at,
                       parse.amount,
                       parse.transaction_type,
                       parse.category,
                       parse.fixed_cost,
                       parse.risk_level
                  FROM raw_spend_event raw_event
                  LEFT JOIN fast_parse_result parse ON parse.raw_event_id = raw_event.id
                 WHERE raw_event.user_id = :userId
                   AND COALESCE(raw_event.occurred_at, raw_event.received_at) >= :from
                   AND COALESCE(raw_event.occurred_at, raw_event.received_at) < :until
                """);
        if (query.status() != null) {
            sql.append(" AND raw_event.status = :status");
        }
        if (query.category() != null) {
            sql.append(" AND parse.category = :category");
        }
        if (query.cursorTransactionAt() != null) {
            sql.append("""
                     AND (COALESCE(raw_event.occurred_at, raw_event.received_at) < :cursorAt
                          OR (COALESCE(raw_event.occurred_at, raw_event.received_at) = :cursorAt
                              AND raw_event.id < :cursorId))
                    """);
        }
        sql.append("""
                 ORDER BY COALESCE(raw_event.occurred_at, raw_event.received_at) DESC,
                          raw_event.id DESC
                 LIMIT :limit
                """);

        JdbcClient.StatementSpec statement = jdbcClient.sql(sql.toString())
                .param("userId", query.userId())
                .param("from", query.from().atOffset(ZoneOffset.UTC))
                .param("until", query.until().atOffset(ZoneOffset.UTC))
                .param("limit", query.limit());
        if (query.status() != null) {
            statement = statement.param("status", query.status().name());
        }
        if (query.category() != null) {
            statement = statement.param("category", query.category());
        }
        if (query.cursorTransactionAt() != null) {
            statement = statement
                    .param("cursorAt", query.cursorTransactionAt().atOffset(ZoneOffset.UTC))
                    .param("cursorId", query.cursorEventId());
        }
        return statement.query(this::mapHistoryItem).list();
    }

    private SpendEventDetail map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SpendEventDetail(
                resultSet.getObject("event_id", UUID.class),
                SpendEventSource.valueOf(resultSet.getString("source")),
                SpendEventStatus.valueOf(resultSet.getString("event_status")),
                instant(resultSet, "occurred_at"),
                instant(resultSet, "received_at"),
                fastParseResult(resultSet)
        );
    }

    private FastParseResult fastParseResult(ResultSet resultSet) throws SQLException {
        String parseStatus = resultSet.getString("parse_status");
        if (parseStatus == null) {
            return null;
        }
        return new FastParseResult(
                resultSet.getBigDecimal("amount"),
                resultSet.getString("transaction_type"),
                parseStatus,
                resultSet.getString("review_reason"),
                resultSet.getString("category"),
                resultSet.getObject("fixed_cost", Boolean.class),
                resultSet.getString("risk_level"),
                resultSet.getString("risk_reason"),
                resultSet.getString("parser_version"),
                instant(resultSet, "parsed_at")
        );
    }

    private SpendEventHistoryItem mapHistoryItem(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new SpendEventHistoryItem(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("sanitized_message"),
                SpendEventStatus.valueOf(resultSet.getString("event_status")),
                instant(resultSet, "transaction_at"),
                resultSet.getBigDecimal("amount"),
                resultSet.getString("transaction_type"),
                resultSet.getString("category"),
                resultSet.getObject("fixed_cost", Boolean.class),
                resultSet.getString("risk_level")
        );
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
