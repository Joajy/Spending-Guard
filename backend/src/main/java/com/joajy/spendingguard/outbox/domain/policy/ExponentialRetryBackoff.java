package com.joajy.spendingguard.outbox.domain.policy;

import java.time.Duration;

/**
 * 연속 실패 횟수에 따라 재시도 대기 시간을 두 배씩 늘리는 도메인 정책이다.
 *
 * <p>{@code baseDelay * 2^previousAttempts}를 계산하되 {@code maxDelay}를 넘지 않는다.
 * 시간 곱셈이 오버플로하면 최대값을 반환해 비정상적인 재시도 시각 생성을 막는다.
 * 이 정책은 jitter를 적용하지 않으므로 다수 인스턴스의 동시 재시도가 문제가 되면 별도
 * 분산 정책을 추가해야 한다.
 */
public class ExponentialRetryBackoff {

    private final Duration baseDelay;
    private final Duration maxDelay;

    /**
     * 기본 지연과 최대 지연으로 재시도 정책을 생성한다.
     *
     * @param baseDelay 첫 실패 이후 적용할 양수 지연
     * @param maxDelay 기본 지연보다 짧지 않은 최대 지연
     * @throws IllegalArgumentException 시간 범위가 정책 조건을 만족하지 않는 경우
     */
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

    /**
     * 이전 실패 횟수 다음에 적용할 재시도 지연을 계산한다.
     *
     * @param previousAttempts 지금까지 기록된 실패 횟수
     * @return 기본 지연 이상, 최대 지연 이하의 재시도 간격
     * @throws IllegalArgumentException 실패 횟수가 음수인 경우
     */
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
