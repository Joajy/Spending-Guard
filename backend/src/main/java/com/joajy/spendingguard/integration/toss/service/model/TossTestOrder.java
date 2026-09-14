package com.joajy.spendingguard.integration.toss.service.model;

import java.time.Instant;
import java.util.UUID;

/** 결제 인증 전 서버에 저장하는 Toss 테스트 주문이다. */
public record TossTestOrder(
        String orderId,
        UUID userId,
        long amount,
        String orderName,
        Instant createdAt
) {
}
