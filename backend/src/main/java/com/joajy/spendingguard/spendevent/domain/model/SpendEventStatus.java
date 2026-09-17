package com.joajy.spendingguard.spendevent.domain.model;

/**
 * 소비 이벤트의 도메인 처리 단계를 나타낸다.
 *
 * <p>이 상태는 Outbox 발행 상태와 분리된다. 소비 데이터의 업무 처리 단계와 메시지
 * 전달 인프라의 수명주기를 섞지 않기 위한 구분이다.
 */
public enum SpendEventStatus {
    /** 원천 데이터와 발행용 Outbox 이벤트가 저장된 상태. */
    RECEIVED,
    /** 빠른 파싱이 끝나고 외부 분석 또는 규칙 보강을 기다리는 상태. */
    ANALYZING,
    /** 분석 결과와 예산 반영까지 정상적으로 끝난 상태. */
    COMPLETED,
    /** 필수 정보를 규칙으로 확정하지 못해 사용자 확인이 필요한 상태. */
    NEEDS_REVIEW
}
