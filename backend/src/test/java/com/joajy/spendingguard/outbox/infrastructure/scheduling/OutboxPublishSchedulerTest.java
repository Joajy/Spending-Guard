package com.joajy.spendingguard.outbox.infrastructure.scheduling;

import com.joajy.spendingguard.outbox.application.result.OutboxPublishBatchResult;
import com.joajy.spendingguard.outbox.application.service.OutboxPublishService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class OutboxPublishSchedulerTest {

    @Test
    void recordsBatchResultAsOperationalMetrics() {
        OutboxPublishService service = mock(OutboxPublishService.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        given(service.publishPending()).willReturn(new OutboxPublishBatchResult(3, 2, 1));
        OutboxPublishScheduler scheduler = new OutboxPublishScheduler(service, meterRegistry);

        scheduler.publishPending();

        assertThat(meterRegistry.counter("spending.guard.outbox.publish.success").count())
                .isEqualTo(2);
        assertThat(meterRegistry.counter("spending.guard.outbox.publish.failure").count())
                .isEqualTo(1);
        assertThat(meterRegistry.timer("spending.guard.outbox.batch.duration").count())
                .isOne();
    }
}

