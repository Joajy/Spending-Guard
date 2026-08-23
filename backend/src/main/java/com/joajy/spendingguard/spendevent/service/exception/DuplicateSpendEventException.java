package com.joajy.spendingguard.spendevent.service.exception;

/**
 * 동일한 소비 알림이 이미 접수되어 중복 방지 제약을 위반했음을 나타낸다.
 *
 * <p><strong>도입 배경:</strong> 재전송, 네트워크 재시도, 동시에 도착한 같은 알림은
 * 정상 운영에서도 발생한다. 이를 알 수 없는 서버 오류로 처리하면 사용자는 다시
 * 요청하게 되고 중복 상황을 더 악화시킬 수 있다.
 *
 * <p><strong>존재 이유:</strong> PostgreSQL의 고유 제약 예외를 유스케이스 의미로
 * 변환한다. HTTP 계층은 저장 기술을 몰라도 이 예외를 {@code 409 Conflict}로 응답할 수
 * 있고, 다른 입력 어댑터도 같은 중복 의미를 재사용할 수 있다.
 */
public class DuplicateSpendEventException extends RuntimeException {

    /** 사용자에게 노출할 수 있는 중복 접수 메시지로 예외를 생성한다. */
    public DuplicateSpendEventException() {
        super("이미 접수된 소비 알림입니다.");
    }
}
