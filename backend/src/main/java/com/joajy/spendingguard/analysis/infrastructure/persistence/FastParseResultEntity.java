package com.joajy.spendingguard.analysis.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.FastParseOutcome;
import com.joajy.spendingguard.analysis.domain.model.FastParseStatus;
import com.joajy.spendingguard.analysis.domain.model.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 외부 AI 호출 전 확보한 금액·거래유형과 검토 사유를 저장하는 JPA 엔티티다. */
@Entity
@Table(name = "fast_parse_result")
public class FastParseResultEntity {

    @Id
    @Column(name = "raw_event_id")
    private UUID rawEventId;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 20)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FastParseStatus status;

    @Column(name = "review_reason", length = 200)
    private String reviewReason;

    @Column(name = "parser_version", nullable = false, length = 50)
    private String parserVersion;

    @Column(name = "parsed_at", nullable = false)
    private Instant parsedAt;

    protected FastParseResultEntity() {
    }

    FastParseResultEntity(
            UUID rawEventId,
            FastParseOutcome outcome,
            String parserVersion,
            Instant parsedAt
    ) {
        this.rawEventId = rawEventId;
        this.amount = outcome.amount();
        this.transactionType = outcome.transactionType();
        this.status = outcome.status();
        this.reviewReason = outcome.reviewReason();
        this.parserVersion = parserVersion;
        this.parsedAt = parsedAt;
    }

    public UUID getRawEventId() {
        return rawEventId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public FastParseStatus getStatus() {
        return status;
    }

    public String getReviewReason() {
        return reviewReason;
    }
}
