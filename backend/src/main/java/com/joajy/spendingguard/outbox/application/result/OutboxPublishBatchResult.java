package com.joajy.spendingguard.outbox.application.result;

public record OutboxPublishBatchResult(int claimed, int published, int failed) {

    public static OutboxPublishBatchResult empty() {
        return new OutboxPublishBatchResult(0, 0, 0);
    }
}

