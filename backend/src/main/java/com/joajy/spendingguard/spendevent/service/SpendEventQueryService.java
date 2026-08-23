package com.joajy.spendingguard.spendevent.service;

import java.util.UUID;

import com.joajy.spendingguard.spendevent.service.exception.SpendEventNotFoundException;
import com.joajy.spendingguard.spendevent.service.port.inbound.GetSpendEventUseCase;
import com.joajy.spendingguard.spendevent.service.port.outbound.LoadSpendEventDetailPort;
import com.joajy.spendingguard.spendevent.service.result.SpendEventDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소비 이벤트의 비동기 처리 상태를 조회하는 유스케이스다.
 *
 * <p>조회 전용 트랜잭션 안에서 원천 이벤트와 현재 파싱 결과의 한 시점 스냅샷을 읽는다.
 * 조회 모델이 없으면 저장소 기술 예외 대신 API에서 해석 가능한 업무 예외로 변환한다.
 */
@Service
public class SpendEventQueryService implements GetSpendEventUseCase {

    private final LoadSpendEventDetailPort loadSpendEventDetailPort;

    public SpendEventQueryService(LoadSpendEventDetailPort loadSpendEventDetailPort) {
        this.loadSpendEventDetailPort = loadSpendEventDetailPort;
    }

    @Override
    @Transactional(readOnly = true)
    public SpendEventDetail get(UUID userId, UUID eventId) {
        return loadSpendEventDetailPort.findByUserIdAndId(userId, eventId)
                .orElseThrow(() -> new SpendEventNotFoundException(eventId));
    }

    public SpendEventDetail get(UUID eventId) {
        return loadSpendEventDetailPort.findById(eventId)
                .orElseThrow(() -> new SpendEventNotFoundException(eventId));
    }
}
