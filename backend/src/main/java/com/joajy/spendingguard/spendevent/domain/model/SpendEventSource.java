package com.joajy.spendingguard.spendevent.domain.model;

/** 소비 이벤트가 유입된 채널을 구분하며 중복 키의 네임스페이스로도 사용한다. */
public enum SpendEventSource {
    /** 사용자가 직접 입력한 소비 알림 텍스트. */
    MANUAL_TEXT,

    /** 개발과 검증을 위해 시뮬레이터가 생성한 소비 이벤트. */
    SIMULATOR,

    /** Toss Payments 상점의 결제 상태 웹훅을 원본 조회로 검증한 이벤트. */
    TOSS_WEBHOOK
}
