package com.joajy.spendingguard.spendevent.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.service.port.outbound.LoadSpendEventDetailPort;
import com.joajy.spendingguard.spendevent.service.result.FastParseResult;
import com.joajy.spendingguard.spendevent.service.result.SpendEventDetail;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * 상태 조회에 필요한 두 테이블을 한 번에 읽는 조회 전용 영속성 어댑터다.
 *
 * <p>쓰기 엔티티 사이에 조회 편의를 위한 연관관계를 추가하지 않고 명시적인 LEFT JOIN을
 * 사용한다. 따라서 분석 전의 이벤트도 반환하며, API 요청 한 번에 추가 조회가 발생하지 않는다.
 */
@Component
class SpendEventQueryPersistenceAdapter implements LoadSpendEventDetailPort {

    private static final String FIND_BY_ID = """
            SELECT raw_event.id AS event_id,
                   raw_event.source,
                   raw_event.status AS event_status,
                   raw_event.occurred_at,
                   raw_event.received_at,
                   parse.amount,
                   parse.transaction_type,
                   parse.status AS parse_status,
                   parse.review_reason,
                   parse.parser_version,
                   parse.parsed_at
              FROM raw_spend_event raw_event
              LEFT JOIN fast_parse_result parse ON parse.raw_event_id = raw_event.id
             WHERE raw_event.id = :eventId
            """;

    private final JdbcClient jdbcClient;

    SpendEventQueryPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<SpendEventDetail> findById(UUID eventId) {
        return jdbcClient.sql(FIND_BY_ID)
                .param("eventId", eventId)
                .query(this::map)
                .optional();
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
                resultSet.getString("parser_version"),
                instant(resultSet, "parsed_at")
        );
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}

