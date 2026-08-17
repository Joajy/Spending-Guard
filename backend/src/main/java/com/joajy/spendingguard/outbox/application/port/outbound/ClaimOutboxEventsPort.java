package com.joajy.spendingguard.outbox.application.port.outbound;

import java.time.Instant;
import java.util.List;

import com.joajy.spendingguard.outbox.application.model.ClaimedOutboxEvent;

/**
 * 발행 가능한 Outbox 이벤트를 배타적으로 선점하기 위한 출력 포트다.
 *
 * <p>한 번 반환한 이벤트는 {@code claimedUntil}까지 현재 작업자의 소유로 취급한다.
 * 구현체는 동시 호출 간에 같은 유효 임대가 중복 반환되지 않도록 보장해야 한다.
 */
public interface ClaimOutboxEventsPort {

    /**
     * 현재 시각을 기준으로 발행할 수 있는 이벤트를 최대 배치 크기만큼 선점한다.
     *
     * @param batchSize 한 번에 선점할 수 있는 최대 건수
     * @param claimedAt 선점 가능 여부를 판단할 기준 시각
     * @param claimedUntil 이번 처리 권한이 만료되는 시각
     * @return 현재 작업자가 처리할 수 있는 이벤트, 대상이 없으면 빈 목록
     */
    List<ClaimedOutboxEvent> claim(int batchSize, Instant claimedAt, Instant claimedUntil);
}
