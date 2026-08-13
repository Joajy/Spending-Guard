package com.joajy.spendingguard.spendevent.application.port.outbound;

import com.joajy.spendingguard.spendevent.domain.model.RawSpendEvent;

public interface StoreRawSpendEventPort {

    void store(RawSpendEvent spendEvent);
}
