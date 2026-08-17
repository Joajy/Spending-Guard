package com.joajy.spendingguard.outbox.application.port.outbound;

import java.time.Instant;
import java.util.List;

import com.joajy.spendingguard.outbox.application.model.ClaimedOutboxEvent;

/**
 * 발행 가능한 Outbox 이벤트를 배타적으로 선점하기 위한 출력 포트다.
 * 애플리케이션은 선점 방식이나 데이터베이스 잠금 구현을 알지 않고 배치 크기와 임대 구간만 지정한다.
 */
public interface ClaimOutboxEventsPort {

    List<ClaimedOutboxEvent> claim(int batchSize, Instant claimedAt, Instant claimedUntil);
}
