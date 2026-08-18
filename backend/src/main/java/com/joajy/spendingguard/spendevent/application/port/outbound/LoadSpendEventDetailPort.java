package com.joajy.spendingguard.spendevent.application.port.outbound;

import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.application.result.SpendEventDetail;

/** 원천 이벤트와 현재 분석 결과를 결합해 읽는 조회 전용 출력 포트다. */
public interface LoadSpendEventDetailPort {

    Optional<SpendEventDetail> findById(UUID eventId);
}

