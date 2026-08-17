package com.joajy.spendingguard.outbox.infrastructure.persistence;

/**
 * Outbox 행이 발행 파이프라인에서 어느 단계에 있는지 나타내는 영속성 상태다.
 * 대기, 처리 중, 발행 완료 상태를 구분해 재시도 대상과 완료 데이터를 명확히 판별한다.
 */
enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED
}
