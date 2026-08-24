package com.joajy.spendingguard.analysis.service.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.FastParseStatus;
import com.joajy.spendingguard.analysis.domain.model.SpendCategory;
import com.joajy.spendingguard.analysis.domain.model.TransactionType;

/** 카테고리 수정 가능 여부와 위험 신호 재계산에 필요한 현재 분석 스냅샷이다. */
public record SpendCategoryCorrectionTarget(
        UUID eventId,
        FastParseStatus parseStatus,
        SpendCategory originalCategory,
        BigDecimal amount,
        TransactionType transactionType,
        Instant occurredAt,
        long version
) {
    public boolean editable() {
        return parseStatus == FastParseStatus.PARSED
                && originalCategory != null
                && amount != null
                && transactionType != null;
    }
}
