package com.joajy.spendingguard.outbox.application.result;

/**
 * 한 번의 Outbox 배치가 선점하고 발행한 결과를 요약한다.
 * 스케줄러가 처리량과 실패 건수를 로그와 지표로 남길 때 내부 이벤트 목록을 노출하지 않도록 한다.
 */
public record OutboxPublishBatchResult(int claimed, int published, int failed) {

    public static OutboxPublishBatchResult empty() {
        return new OutboxPublishBatchResult(0, 0, 0);
    }
}
