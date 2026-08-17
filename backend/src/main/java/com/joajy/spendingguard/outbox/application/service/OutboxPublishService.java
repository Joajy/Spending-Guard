package com.joajy.spendingguard.outbox.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.joajy.spendingguard.outbox.application.exception.OutboxPublishException;
import com.joajy.spendingguard.outbox.application.model.ClaimedOutboxEvent;
import com.joajy.spendingguard.outbox.application.port.outbound.ClaimOutboxEventsPort;
import com.joajy.spendingguard.outbox.application.port.outbound.PublishOutboxEventPort;
import com.joajy.spendingguard.outbox.application.port.outbound.UpdateOutboxEventStatePort;
import com.joajy.spendingguard.outbox.application.result.OutboxPublishBatchResult;
import org.springframework.stereotype.Service;

/**
 * 대기 중인 Outbox 이벤트를 선점하고 외부 브로커에 발행한 뒤 결과 상태를 기록하는 유스케이스다.
 * 개별 이벤트의 발행 실패는 다음 재시도 시각을 계산해 격리하며, 한 건의 실패가 같은 배치의 나머지 발행을 막지 않게 한다.
 * 실제 선점, 전송, 상태 저장은 출력 포트에 위임해 처리 흐름과 인프라 구현을 분리한다.
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
