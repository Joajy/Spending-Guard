package com.joajy.spendingguard.analysis.service.port.outbound;

import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.analysis.service.model.SpendCategoryCorrectionTarget;

/** 사용자 소유 이벤트의 자동 분류와 현재 수정 버전을 읽는 출력 포트다. */
public interface LoadSpendCategoryCorrectionTargetPort {
    Optional<SpendCategoryCorrectionTarget> find(UUID userId, UUID eventId);
}
