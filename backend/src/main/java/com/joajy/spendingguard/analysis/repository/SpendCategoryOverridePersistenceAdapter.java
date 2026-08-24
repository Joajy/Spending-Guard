package com.joajy.spendingguard.analysis.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.FastParseStatus;
import com.joajy.spendingguard.analysis.domain.model.SpendCategory;
import com.joajy.spendingguard.analysis.domain.model.SpendRiskAssessment;
import com.joajy.spendingguard.analysis.domain.model.TransactionType;
import com.joajy.spendingguard.analysis.service.model.SpendCategoryCorrectionTarget;
import com.joajy.spendingguard.analysis.service.port.outbound.LoadSpendCategoryCorrectionTargetPort;
import com.joajy.spendingguard.analysis.service.port.outbound.StoreSpendCategoryOverridePort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** 자동 분석 결과를 보존하면서 사용자 수정값을 버전 조건부로 저장하는 영속성 어댑터다. */
@Component
class SpendCategoryOverridePersistenceAdapter implements
        LoadSpendCategoryCorrectionTargetPort,
        StoreSpendCategoryOverridePort {

    private static final String FIND_TARGET = """
            SELECT raw_event.id AS event_id,
                   raw_event.occurred_at,
                   parse.status AS parse_status,
                   parse.category AS original_category,
                   parse.amount,
                   parse.transaction_type,
                   COALESCE(category_override.version, 0) AS category_version
              FROM raw_spend_event raw_event
              LEFT JOIN fast_parse_result parse ON parse.raw_event_id = raw_event.id
              LEFT JOIN spend_category_override category_override
                ON category_override.spend_event_id = raw_event.id
             WHERE raw_event.user_id = :userId
               AND raw_event.id = :eventId
            """;

    private static final String UPSERT_OVERRIDE = """
            INSERT INTO spend_category_override (
                spend_event_id, user_id, category, fixed_cost,
                risk_level, risk_reason, version, corrected_at
            )
            SELECT :eventId, :userId, :category, :fixedCost,
                   :riskLevel, :riskReason, 1, :correctedAt
             WHERE :expectedVersion = 0
                OR EXISTS (
                    SELECT 1
                      FROM spend_category_override current_override
                     WHERE current_override.spend_event_id = :eventId
                )
            ON CONFLICT (spend_event_id) DO UPDATE
               SET category = EXCLUDED.category,
                   fixed_cost = EXCLUDED.fixed_cost,
                   risk_level = EXCLUDED.risk_level,
                   risk_reason = EXCLUDED.risk_reason,
                   version = spend_category_override.version + 1,
                   corrected_at = EXCLUDED.corrected_at
             WHERE spend_category_override.user_id = :userId
               AND spend_category_override.version = :expectedVersion
            RETURNING version
            """;

    private final JdbcClient jdbcClient;

    SpendCategoryOverridePersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<SpendCategoryCorrectionTarget> find(UUID userId, UUID eventId) {
        return jdbcClient.sql(FIND_TARGET)
                .param("userId", userId)
                .param("eventId", eventId)
                .query(this::mapTarget)
                .optional();
    }

    @Override
    public OptionalLong store(
            UUID userId,
            UUID eventId,
            SpendRiskAssessment assessment,
            long expectedVersion,
            Instant correctedAt
    ) {
        Optional<Long> version = jdbcClient.sql(UPSERT_OVERRIDE)
                .param("eventId", eventId)
                .param("userId", userId)
                .param("category", assessment.category().name())
                .param("fixedCost", assessment.fixedCost())
                .param("riskLevel", assessment.riskLevel().name())
                .param("riskReason", assessment.reasonCode())
                .param("expectedVersion", expectedVersion)
                .param("correctedAt", correctedAt.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .optional();
        return version.isPresent() ? OptionalLong.of(version.get()) : OptionalLong.empty();
    }

    private SpendCategoryCorrectionTarget mapTarget(ResultSet resultSet, int rowNumber)
            throws SQLException {
        String parseStatus = resultSet.getString("parse_status");
        String originalCategory = resultSet.getString("original_category");
        OffsetDateTime occurredAt = resultSet.getObject("occurred_at", OffsetDateTime.class);
        return new SpendCategoryCorrectionTarget(
                resultSet.getObject("event_id", UUID.class),
                parseStatus == null ? null : FastParseStatus.valueOf(parseStatus),
                originalCategory == null ? null : SpendCategory.valueOf(originalCategory),
                resultSet.getBigDecimal("amount"),
                transactionType(resultSet.getString("transaction_type")),
                occurredAt == null ? null : occurredAt.toInstant(),
                resultSet.getLong("category_version")
        );
    }

    private TransactionType transactionType(String value) {
        return value == null ? null : TransactionType.valueOf(value);
    }
}
