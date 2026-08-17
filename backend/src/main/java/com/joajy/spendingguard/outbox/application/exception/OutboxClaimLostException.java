package com.joajy.spendingguard.outbox.application.exception;

import java.util.UUID;

public class OutboxClaimLostException extends RuntimeException {

    public OutboxClaimLostException(UUID eventId) {
        super("Outbox event claim is no longer valid: " + eventId);
    }
}

