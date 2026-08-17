package com.joajy.spendingguard.spendevent.application.port.inbound;

import com.joajy.spendingguard.spendevent.application.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.application.result.SpendEventReceipt;

/**
 * 외부 수집 채널이 소비 이벤트를 접수할 때 사용하는 입력 포트다.
 *
 * <p>정상 반환은 원천 이벤트와 발행용 Outbox 이벤트가 같은 트랜잭션에 저장되었다는
 * 의미다. 이후 분류나 위험 분석의 완료까지 보장하지 않는다.
 */
public interface SubmitSpendEventUseCase {

    /**
     * 소비 알림 하나를 비동기 분석 파이프라인에 접수한다.
     *
     * @param command 수집 채널과 정제 전 소비 알림
     * @return 새 이벤트의 식별자와 접수 상태
     * @throws com.joajy.spendingguard.spendevent.application.exception.DuplicateSpendEventException
     *     같은 알림이 이미 접수된 경우
     */
    SpendEventReceipt submit(SubmitSpendEventCommand command);
}
