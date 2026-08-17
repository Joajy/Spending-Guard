package com.joajy.spendingguard.spendevent.application.port.outbound;

import com.joajy.spendingguard.spendevent.domain.event.SpendEventReceived;

/**
 * 접수 완료 이벤트를 신뢰성 있게 발행할 저장 영역에 추가하는 출력 포트다.
 * 애플리케이션은 Outbox 테이블과 직렬화 형식을 알지 않고 발행할 도메인 이벤트만 전달한다.
 */
public interface AppendSpendEventOutboxPort {

    void append(SpendEventReceived event);
}
