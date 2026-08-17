package com.joajy.spendingguard.outbox.domain.policy;

import java.time.Duration;

public class ExponentialRetryBackoff {

    private final Duration baseDelay;
    private final Duration maxDelay;

    public ExponentialRetryBackoff(Duration baseDelay, Duration maxDelay) {
        if (baseDelay.isZero() || baseDelay.isNegative()) {
            throw new IllegalArgumentException("baseDelay must be positive");
        }
        if (maxDelay.compareTo(baseDelay) < 0) {
            throw new IllegalArgumentException("maxDelay must not be shorter than baseDelay");
        }
        this.baseDelay = baseDelay;
        this.maxDelay = maxDelay;
    }

    public Duration delayAfter(int previousAttempts) {
        if (previousAttempts < 0) {
            throw new IllegalArgumentException("previousAttempts must not be negative");
        }

        Duration delay = baseDelay;
        for (int attempt = 0; attempt < previousAttempts; attempt++) {
            try {
                Duration doubled = delay.multipliedBy(2);
                if (doubled.compareTo(maxDelay) >= 0) {
                    return maxDelay;
                }
                delay = doubled;
            } catch (ArithmeticException exception) {
                return maxDelay;
            }
        }
        return delay;
    }
}

