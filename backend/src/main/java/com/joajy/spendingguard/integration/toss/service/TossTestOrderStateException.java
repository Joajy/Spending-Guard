package com.joajy.spendingguard.integration.toss.service;

/** 주문이 없거나 이미 승인 중·승인 완료여서 다시 선점할 수 없을 때 발생한다. */
public class TossTestOrderStateException extends RuntimeException {

    public TossTestOrderStateException() {
        super("테스트 주문을 확인할 수 없거나 이미 승인 처리된 주문입니다.");
    }
}
