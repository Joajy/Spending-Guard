package com.joajy.spendingguard.spendevent.application.port.inbound;

import com.joajy.spendingguard.spendevent.application.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.application.result.SpendEventReceipt;

public interface SubmitSpendEventUseCase {

    SpendEventReceipt submit(SubmitSpendEventCommand command);
}
