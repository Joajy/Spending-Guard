package com.joajy.spendingguard.spendevent.domain.model;

/**
 * 소비 이벤트가 유입된 채널을 구분한다.
 *
 * <p>중복 키의 네임스페이스에도 포함되므로 같은 외부 ID라도 채널이 다르면 별개
 * 이벤트로 취급한다.
 */
public enum SpendEventSource {
    /** 사용자가 직접 입력한 소비 알림 텍스트. */
    MANUAL_TEXT,

    /** 개발과 검증을 위해 시뮬레이터가 생성한 소비 이벤트. */
    SIMULATOR
}
