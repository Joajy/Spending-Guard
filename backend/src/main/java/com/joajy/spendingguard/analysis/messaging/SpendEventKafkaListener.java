package com.joajy.spendingguard.analysis.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joajy.spendingguard.analysis.service.command.ProcessSpendEventCommand;
import com.joajy.spendingguard.analysis.service.port.inbound.ProcessSpendEventUseCase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Outbox가 발행한 소비 이벤트를 Kafka에서 받아 빠른 분석 유스케이스로 전달한다.
 *
 * <p>Listener가 정상 반환한 뒤에만 record 단위 오프셋이 커밋된다. 일시적 예외는 공통
 * 오류 처리기가 재시도하고, 메시지 계약 위반이나 재시도 소진 건은 DLQ로 보낸다.
 */
@Component
@ConditionalOnProperty(
        prefix = "spending-guard.analysis.consumer",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
class SpendEventKafkaListener {

    private final ObjectMapper objectMapper;
    private final ProcessSpendEventUseCase processSpendEventUseCase;
    private final MeterRegistry meterRegistry;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer processingTimer;

    SpendEventKafkaListener(
            ObjectMapper objectMapper,
            ProcessSpendEventUseCase processSpendEventUseCase,
            MeterRegistry meterRegistry
    ) {
        this.objectMapper = objectMapper;
        this.processSpendEventUseCase = processSpendEventUseCase;
        this.meterRegistry = meterRegistry;
        this.successCounter = meterRegistry.counter("spending.guard.analysis.consume.success");
        this.failureCounter = meterRegistry.counter("spending.guard.analysis.consume.failure");
        this.processingTimer = meterRegistry.timer("spending.guard.analysis.consume.duration");
    }

    @KafkaListener(
            topics = "${spending-guard.analysis.consumer.topic}",
            groupId = "${spending-guard.analysis.consumer.group-id}"
    )
    void consume(String payload) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            SpendEventReceivedMessage message = deserialize(payload);
            processSpendEventUseCase.process(new ProcessSpendEventCommand(message.eventId()));
            successCounter.increment();
        } catch (RuntimeException exception) {
            failureCounter.increment();
            throw exception;
        } finally {
            sample.stop(processingTimer);
        }
    }

    private SpendEventReceivedMessage deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, SpendEventReceivedMessage.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new InvalidSpendEventMessageException(exception);
        }
    }
}
