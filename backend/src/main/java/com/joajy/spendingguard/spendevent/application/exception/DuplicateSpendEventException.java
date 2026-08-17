package com.joajy.spendingguard.spendevent.application.exception;

/**
 * 동일한 소비 알림이 이미 접수되어 중복 방지 제약을 위반했음을 나타낸다.
 * 데이터베이스 예외를 유스케이스 의미로 변환해 API가 저장 기술에 의존하지 않고 충돌 응답을 만들 수 있게 한다.
 */
public class DuplicateSpendEventException extends RuntimeException {

    /** 사용자에게 노출할 수 있는 중복 접수 메시지로 예외를 생성한다. */
    public DuplicateSpendEventException() {
        super("이미 접수된 소비 알림입니다.");
    }
}
