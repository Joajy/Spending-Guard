package com.joajy.spendingguard.spendevent.service.port.outbound;

import java.util.UUID;

public interface CheckSpendEventOwnerPort {
    boolean exists(UUID userId);
}
