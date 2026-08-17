package com.joajy.spendingguard.spendevent.domain.model;

/**
 * 소비 이벤트의 도메인 처리 단계를 나타낸다.
 * 현재 범위에서는 안전하게 접수된 상태만 표현하며, 분석 파이프라인이 추가되면 상태 전이를 이 모델에서 확장한다.
 */
public enum SpendEventStatus {
    RECEIVED
}
