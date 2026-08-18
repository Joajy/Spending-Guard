package com.joajy.spendingguard.analysis.application.port.outbound;

import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;

/** 빠른 분석 결과에 맞춰 원천 이벤트의 처리 상태를 변경하는 출력 포트다. */
public interface UpdateSpendEventStatusPort {

    void update(UUID eventId, SpendEventStatus status);
}
