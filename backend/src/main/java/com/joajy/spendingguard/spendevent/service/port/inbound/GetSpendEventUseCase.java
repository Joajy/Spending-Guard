package com.joajy.spendingguard.spendevent.service.port.inbound;

import java.util.UUID;

import com.joajy.spendingguard.spendevent.service.result.SpendEventDetail;

/** 접수된 소비 이벤트 한 건의 현재 상태를 조회하는 입력 포트다. */
public interface GetSpendEventUseCase {

    SpendEventDetail get(UUID userId, UUID eventId);
}
