package com.joajy.spendingguard.spendevent.application.port.outbound;

import com.joajy.spendingguard.spendevent.domain.event.SpendEventReceived;

public interface AppendSpendEventOutboxPort {

    void append(SpendEventReceived event);
}
