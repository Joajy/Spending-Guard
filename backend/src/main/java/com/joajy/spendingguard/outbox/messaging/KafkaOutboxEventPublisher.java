package com.joajy.spendingguard.outbox.messaging;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.joajy.spendingguard.outbox.service.exception.OutboxPublishException;
import com.joajy.spendingguard.outbox.service.model.ClaimedOutboxEvent;
import com.joajy.spendingguard.outbox.service.port.outbound.PublishOutboxEventPort;
import com.joajy.spendingguard.outbox.config.OutboxPublisherProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Outbox 이벤트를 Kafka로 전달하는 메시징 출력 어댑터다.
 *
 * <p><strong>파티셔닝:</strong> aggregate ID를 메시지 키로 사용한다. 같은 소비 이벤트에서
 * 파생된 메시지는 동일 파티션으로 전달되어 Kafka가 제공하는 파티션 내 순서를 따른다.
 *
 * <p><strong>성공 기준:</strong> {@link KafkaTemplate#send}의 완료를 설정된 시간까지 기다린다.
 * 단순 전송 요청이 아니라 브로커 확인까지 끝나야 애플리케이션에 성공을 반환한다.
 *
 * <p><strong>실패 변환:</strong> 스레드 중단, 시간 초과, 브로커 실패를 안정적인 오류 코드로
 * 구분한다. 중단 상태는 복원해 상위 실행 환경의 취소 신호를 잃지 않는다.
 */
@Component
class KafkaOutboxEventPublisher implements PublishOutboxEventPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxPublisherProperties properties;

    KafkaOutboxEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            OutboxPublisherProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(ClaimedOutboxEvent event) {
        try {
            kafkaTemplate.send(
                    properties.topic(),
                    event.aggregateId().toString(),
                    event.payload()
            ).get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OutboxPublishException("KAFKA_SEND_INTERRUPTED", exception);
        } catch (TimeoutException exception) {
            throw new OutboxPublishException("KAFKA_SEND_TIMEOUT", exception);
        } catch (ExecutionException exception) {
            throw new OutboxPublishException("KAFKA_SEND_FAILED", exception.getCause());
        }
    }
}
