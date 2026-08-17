package com.joajy.spendingguard.outbox.domain.policy;

import java.time.Duration;

/**
 * 연속 실패 횟수에 따라 재시도 대기 시간을 두 배씩 늘리는 도메인 정책이다.
 * 최대 대기 시간을 상한으로 두고 시간 계산의 오버플로도 상한값으로 처리해 비정상적인 재시도 시각을 방지한다.
 */
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
