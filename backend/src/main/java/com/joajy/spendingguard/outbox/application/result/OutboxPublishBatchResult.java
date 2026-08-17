package com.joajy.spendingguard.outbox.application.result;

/**
 * 한 번의 Outbox 배치가 선점하고 발행한 결과를 요약한다.
 *
 * <p>정상 발행 흐름에서 {@code claimed = published + failed} 관계를 가진다.
 * 스케줄러는 개별 이벤트를 노출하지 않고 이 값만으로 처리량과 실패 지표를 기록한다.
 *
 * @param claimed 이번 배치가 처리 권한을 얻은 전체 건수
 * @param published Kafka 확인까지 완료하고 발행 완료로 기록한 건수
 * @param failed 재시도 대기 상태로 되돌린 건수
 */
public record OutboxPublishBatchResult(int claimed, int published, int failed) {

    /**
     * 발행 대상이 없었던 배치 결과를 생성한다.
     *
     * @return 모든 집계값이 0인 결과
     */
    public static OutboxPublishBatchResult empty() {
        return new OutboxPublishBatchResult(0, 0, 0);
    }
}
