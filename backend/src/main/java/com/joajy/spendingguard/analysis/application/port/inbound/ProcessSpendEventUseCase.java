package com.joajy.spendingguard.analysis.application.port.inbound;

import com.joajy.spendingguard.analysis.application.command.ProcessSpendEventCommand;
import com.joajy.spendingguard.analysis.application.result.SpendEventProcessingResult;

/** Kafka로 전달된 소비 이벤트를 멱등하게 빠른 분석하는 입력 포트다. */
public interface ProcessSpendEventUseCase {

    SpendEventProcessingResult process(ProcessSpendEventCommand command);
}
