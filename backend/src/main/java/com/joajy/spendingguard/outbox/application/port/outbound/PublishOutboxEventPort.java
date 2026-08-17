package com.joajy.spendingguard.outbox.application.port.outbound;

import com.joajy.spendingguard.outbox.application.model.ClaimedOutboxEvent;

public interface PublishOutboxEventPort {

    void publish(ClaimedOutboxEvent event);
}

