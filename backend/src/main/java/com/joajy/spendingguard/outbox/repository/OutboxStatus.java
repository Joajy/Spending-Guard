package com.joajy.spendingguard.outbox.repository;

/**
 * Outbox 행이 발행 파이프라인에서 어느 단계에 있는지 나타내는 영속성 상태다.
 *
 * <p>업무 도메인의 소비 이벤트 상태와 무관한 메시지 전달 전용 수명주기다.
 */
enum OutboxStatus {
    /** 최초 발행 또는 실패 후 재시도 시각을 기다리는 상태. */
    PENDING,

    /** 특정 작업자가 claim token과 만료 시각으로 처리 권한을 가진 상태. */
    PROCESSING,

    /** 브로커 전송 확인과 완료 시각 기록이 끝난 최종 상태. */
    PUBLISHED
}
