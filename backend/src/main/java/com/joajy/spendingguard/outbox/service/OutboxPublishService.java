package com.joajy.spendingguard.outbox.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.joajy.spendingguard.outbox.service.exception.OutboxPublishException;
import com.joajy.spendingguard.outbox.service.model.ClaimedOutboxEvent;
import com.joajy.spendingguard.outbox.service.port.outbound.ClaimOutboxEventsPort;
import com.joajy.spendingguard.outbox.service.port.outbound.PublishOutboxEventPort;
import com.joajy.spendingguard.outbox.service.port.outbound.UpdateOutboxEventStatePort;
import com.joajy.spendingguard.outbox.service.result.OutboxPublishBatchResult;
import org.springframework.stereotype.Service;

/**
 * 대기 중인 Outbox 이벤트를 선점하고 외부 브로커에 발행한 뒤 결과 상태를 기록하는 유스케이스다.
 *
 * <p><strong>처리 순서:</strong> 실행 시각을 기준으로 제한된 수의 이벤트를 선점하고,
 * 각 이벤트를 순차 발행한 뒤 성공 또는 재시도 상태를 별도 트랜잭션으로 기록한다.
 *
 * <p><strong>실패 격리:</strong> 브로커 발행 실패는 지수 백오프로 다음 시각을 계산하고
 * 같은 배치의 다음 이벤트 처리를 계속한다. 반면 선점 권한 상실이나 영속성 실패는 배치
 * 경계까지 전파해 운영 지표에서 배치 실패로 구분한다.
 *
 * <p><strong>전달 보장:</strong> 데이터베이스 상태와 Kafka 발행 사이에 원자적 커밋은 없다.
 * 발행 후 상태 기록 전에 프로세스가 종료되면 같은 이벤트가 다시 발행될 수 있으므로,
 * 소비자는 이벤트 ID를 기준으로 멱등 처리해야 한다.
 */
@Service
public class OutboxPublishService {

    private final ClaimOutboxEventsPort claimOutboxEventsPort;
    private final PublishOutboxEventPort publishOutboxEventPort;
    private final UpdateOutboxEventStatePort updateOutboxEventStatePort;
    private final OutboxPublishPolicy policy;
    private final Clock clock;

    public OutboxPublishService(
            ClaimOutboxEventsPort claimOutboxEventsPort,
            PublishOutboxEventPort publishOutboxEventPort,
            UpdateOutboxEventStatePort updateOutboxEventStatePort,
            OutboxPublishPolicy policy,
            Clock clock
    ) {
        this.claimOutboxEventsPort = claimOutboxEventsPort;
        this.publishOutboxEventPort = publishOutboxEventPort;
        this.updateOutboxEventStatePort = updateOutboxEventStatePort;
        this.policy = policy;
        this.clock = clock;
    }

    /**
     * 현재 발행 가능한 이벤트 한 배치를 처리한다.
     *
     * @return 선점, 발행 완료, 재시도 전환 건수
     */
    public OutboxPublishBatchResult publishPending() {
        Instant claimedAt = clock.instant();
        List<ClaimedOutboxEvent> events = claimOutboxEventsPort.claim(
                policy.batchSize(),
                claimedAt,
                claimedAt.plus(policy.leaseDuration())
        );
        if (events.isEmpty()) {
            return OutboxPublishBatchResult.empty();
        }

        int published = 0;
        int failed = 0;
        for (ClaimedOutboxEvent event : events) {
            try {
                publishOutboxEventPort.publish(event);
                updateOutboxEventStatePort.markPublished(
                        event.id(),
                        event.claimToken(),
                        clock.instant()
                );
                published++;
            } catch (OutboxPublishException exception) {
                Instant failedAt = clock.instant();
                updateOutboxEventStatePort.markFailed(
                        event.id(),
                        event.claimToken(),
                        failedAt.plus(policy.retryBackoff().delayAfter(event.attemptCount())),
                        exception.getErrorCode()
                );
                failed++;
            }
        }

        return new OutboxPublishBatchResult(events.size(), published, failed);
    }
}
