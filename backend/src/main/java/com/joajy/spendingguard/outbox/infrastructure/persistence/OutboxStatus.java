package com.joajy.spendingguard.outbox.infrastructure.persistence;

enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED
}

