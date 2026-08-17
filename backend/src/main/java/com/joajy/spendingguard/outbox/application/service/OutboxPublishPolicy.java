package com.joajy.spendingguard.outbox.application.service;

import java.time.Duration;

import com.joajy.spendingguard.outbox.domain.policy.ExponentialRetryBackoff;

/**
 * Outbox 발행 유스케이스에 필요한 배치 크기, 처리 권한의 임대 시간, 재시도 간격을 묶는다.
 *
 * <p>생성 시 배치 크기와 임대 시간이 양수인지 검사한다. 잘못된 운영 설정을 시작
 * 단계에서 거부해 발행 루프가 무한 대기하거나 빈 배치만 반복하는 상태를 방지한다.
 *
 * @param batchSize 한 번의 실행에서 선점할 수 있는 최대 이벤트 수
 * @param leaseDuration 작업자가 이벤트 상태를 확정할 수 있는 처리 권한의 유효 시간
 * @param retryBackoff 실패 횟수로 다음 시도 지연 시간을 계산하는 정책
 */
public record OutboxPublishPolicy(
        int batchSize,
        Duration leaseDuration,
        ExponentialRetryBackoff retryBackoff
) {

    /**
     * 운영 설정에서 전달된 값이 발행 루프의 기본 조건을 만족하는지 검증한다.
     *
     * @throws IllegalArgumentException 배치 크기나 임대 시간이 양수가 아닌 경우
     */
    public OutboxPublishPolicy {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }
    }
}
