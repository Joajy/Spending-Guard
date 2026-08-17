package com.joajy.spendingguard.spendevent.domain.model;

/**
 * 소비 이벤트가 유입된 채널을 구분한다.
 * 현재는 직접 입력과 개발용 시뮬레이터를 지원하며, 향후 금융 연동 채널을 추가할 때도 같은 접수 흐름을 재사용한다.
 */
public enum SpendEventSource {
    MANUAL_TEXT,
    SIMULATOR
}
