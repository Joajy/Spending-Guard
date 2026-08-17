package com.joajy.spendingguard.outbox.application.service;

import java.time.Duration;

import com.joajy.spendingguard.outbox.domain.policy.ExponentialRetryBackoff;

/**
 * Outbox 발행 유스케이스에 필요한 배치 크기, 처리 권한의 임대 시간, 재시도 간격을 묶는다.
 * 생성 시 기본 불변식을 검사해 잘못된 운영 설정이 실제 발행 루프까지 전달되지 않게 한다.
 */
public record OutboxPublishPolicy(
        int batchSize,
        Duration leaseDuration,
        ExponentialRetryBackoff retryBackoff
) {

    public OutboxPublishPolicy {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }
    }
}
