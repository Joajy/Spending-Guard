package com.joajy.spendingguard.analysis.service.port.outbound;

import java.time.Instant;
import java.util.OptionalLong;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.SpendRiskAssessment;

/** 기대 버전이 일치할 때만 사용자 분류 수정값을 저장하는 출력 포트다. */
public interface StoreSpendCategoryOverridePort {
    OptionalLong store(
            UUID userId,
            UUID eventId,
            SpendRiskAssessment assessment,
            long expectedVersion,
            Instant correctedAt
    );
}
