package com.joajy.spendingguard.analysis.application.port.outbound;

import java.time.Instant;
import java.util.UUID;

/** 동일 Kafka 이벤트를 같은 Consumer가 한 번만 반영하도록 처리 권한을 선점하는 포트다. */
public interface TryClaimProcessedEventPort {

    boolean tryClaim(UUID eventId, String consumerName, Instant processedAt);
}
