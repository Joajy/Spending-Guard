package com.joajy.spendingguard.outbox.application.port.outbound;

import com.joajy.spendingguard.outbox.application.model.ClaimedOutboxEvent;

/**
 * 선점한 Outbox 이벤트를 외부 메시지 시스템에 전달하는 출력 포트다.
 *
 * <p>정상 반환은 브로커가 전송을 확인했다는 의미다. 확인할 수 없는 실패는
 * {@link com.joajy.spendingguard.outbox.application.exception.OutboxPublishException}으로
 * 변환해 호출자가 재시도 상태를 기록할 수 있게 한다.
 */
public interface PublishOutboxEventPort {

    /**
     * 선점된 이벤트를 외부 브로커에 발행하고 전송 확인까지 기다린다.
     *
     * @param event 현재 작업자가 처리 권한을 가진 이벤트
     * @throws com.joajy.spendingguard.outbox.application.exception.OutboxPublishException
     *     브로커가 제한 시간 안에 발행을 확인하지 못한 경우
     */
    void publish(ClaimedOutboxEvent event);
}
