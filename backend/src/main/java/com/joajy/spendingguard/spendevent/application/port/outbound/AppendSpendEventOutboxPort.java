package com.joajy.spendingguard.spendevent.application.port.outbound;

import com.joajy.spendingguard.spendevent.domain.event.SpendEventReceived;

/**
 * 접수 완료 이벤트를 신뢰성 있게 발행할 저장 영역에 추가하는 출력 포트다.
 *
 * <p>호출자의 트랜잭션에 참여해야 하며, 원천 이벤트 저장과 독립적으로 커밋해서는 안 된다.
 */
public interface AppendSpendEventOutboxPort {

    /**
     * 접수 완료 이벤트를 외부 발행 대기열에 기록한다.
     *
     * @param event 발행할 최소 식별 정보만 포함한 도메인 이벤트
     */
    void append(SpendEventReceived event);
}
