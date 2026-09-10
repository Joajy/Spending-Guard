package com.joajy.spendingguard.integration.toss.service.port.outbound;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.integration.toss.service.model.TossTestOrder;

/** Toss 테스트 주문의 생성과 단일 승인 선점을 제공한다. */
public interface TossTestOrderPort {

    void create(TossTestOrder order);

    Optional<TossTestOrder> claimForConfirmation(
            UUID userId,
            String orderId,
            long amount,
            Instant claimedAt
    );

    void markConfirmed(String orderId, String paymentKey, Instant confirmedAt);

    void releaseConfirmation(String orderId);

    Optional<TossTestOrder> claimForCancellation(
            UUID userId,
            String orderId,
            String paymentKey,
            Instant claimedAt
    );

    void markCanceled(String orderId, Instant canceledAt);

    void releaseCancellation(String orderId);
}
