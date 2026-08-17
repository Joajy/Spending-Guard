package com.joajy.spendingguard.outbox.application.port.outbound;

import com.joajy.spendingguard.outbox.application.model.ClaimedOutboxEvent;

/**
 * 선점한 Outbox 이벤트를 외부 메시지 시스템에 전달하는 출력 포트다.
 * Kafka 같은 전송 기술을 애플리케이션 서비스에서 분리해 발행 수단을 교체하거나 테스트 대역으로 검증할 수 있게 한다.
 */
public interface PublishOutboxEventPort {

    void publish(ClaimedOutboxEvent event);
}
