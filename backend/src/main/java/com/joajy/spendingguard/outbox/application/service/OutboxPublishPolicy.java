package com.joajy.spendingguard.outbox.application.service;

import java.time.Duration;

import com.joajy.spendingguard.outbox.domain.policy.ExponentialRetryBackoff;

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

