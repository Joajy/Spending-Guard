package com.joajy.spendingguard.outbox.scheduling;

import com.joajy.spendingguard.outbox.service.result.OutboxPublishBatchResult;
import com.joajy.spendingguard.outbox.service.OutboxPublishService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 설정된 간격마다 Outbox 발행 유스케이스를 시작하는 스케줄링 어댑터다.
 *
 * <p>{@code fixedDelay} 방식이므로 이전 실행이 끝난 뒤 설정된 시간만큼 기다리고 다음
 * 배치를 시작한다. 한 인스턴스 안에서 실행이 겹치지 않으며, 여러 인스턴스의 경쟁은
 * 영속성 어댑터의 임대 기반 선점이 조정한다.
 *
 * <p>발행 성공·실패 건수, 배치 자체의 실패 횟수, 실행 시간을 Micrometer로 기록한다.
 * 예상하지 못한 예외는 스케줄러 경계에서 기록해 다음 주기의 실행이 중단되지 않게 한다.
 */
@Component
@ConditionalOnProperty(
        name = "spending-guard.outbox.publisher.enabled",
        havingValue = "true",
        matchIfMissing = true
)
class OutboxPublishScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublishScheduler.class);

    private final OutboxPublishService outboxPublishService;
    private final MeterRegistry meterRegistry;
    private final Counter publishedCounter;
    private final Counter failedCounter;
    private final Counter batchFailureCounter;
    private final Timer batchTimer;

    OutboxPublishScheduler(OutboxPublishService outboxPublishService, MeterRegistry meterRegistry) {
        this.outboxPublishService = outboxPublishService;
        this.meterRegistry = meterRegistry;
        this.publishedCounter = meterRegistry.counter("spending.guard.outbox.publish.success");
        this.failedCounter = meterRegistry.counter("spending.guard.outbox.publish.failure");
        this.batchFailureCounter = meterRegistry.counter("spending.guard.outbox.batch.failure");
        this.batchTimer = meterRegistry.timer("spending.guard.outbox.batch.duration");
    }

    @Scheduled(fixedDelayString = "${spending-guard.outbox.publisher.fixed-delay:1000}")
    void publishPending() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            OutboxPublishBatchResult result = outboxPublishService.publishPending();
            publishedCounter.increment(result.published());
            failedCounter.increment(result.failed());
            if (result.claimed() > 0) {
                log.info(
                        "Outbox batch completed: claimed={}, published={}, failed={}",
                        result.claimed(),
                        result.published(),
                        result.failed()
                );
            }
        } catch (RuntimeException exception) {
            batchFailureCounter.increment();
            log.error("Outbox batch failed before completion", exception);
        } finally {
            sample.stop(batchTimer);
        }
    }
}
