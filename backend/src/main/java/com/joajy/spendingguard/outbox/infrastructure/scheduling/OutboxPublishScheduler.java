package com.joajy.spendingguard.outbox.infrastructure.scheduling;

import com.joajy.spendingguard.outbox.application.result.OutboxPublishBatchResult;
import com.joajy.spendingguard.outbox.application.service.OutboxPublishService;
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
 * 배치별 성공·실패 건수와 수행 시간을 Micrometer 지표로 남기고, 예기치 않은 배치 예외가 다음 실행까지 중단시키지 않도록 경계에서 처리한다.
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
