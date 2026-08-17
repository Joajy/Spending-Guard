package com.joajy.spendingguard.outbox.application.port.outbound;

import java.time.Instant;
import java.util.List;

import com.joajy.spendingguard.outbox.application.model.ClaimedOutboxEvent;

public interface ClaimOutboxEventsPort {

    List<ClaimedOutboxEvent> claim(int batchSize, Instant claimedAt, Instant claimedUntil);
}

