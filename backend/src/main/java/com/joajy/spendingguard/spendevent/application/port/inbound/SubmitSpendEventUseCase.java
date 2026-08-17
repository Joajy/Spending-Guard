package com.joajy.spendingguard.spendevent.application.port.inbound;

import com.joajy.spendingguard.spendevent.application.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.application.result.SpendEventReceipt;

/**
 * 외부 수집 채널이 소비 이벤트를 접수할 때 사용하는 입력 포트다.
 * 컨트롤러는 구체 서비스 대신 이 계약에 의존하므로 애플리케이션의 공개 기능과 구현을 분리할 수 있다.
 */
public interface SubmitSpendEventUseCase {

    SpendEventReceipt submit(SubmitSpendEventCommand command);
}
