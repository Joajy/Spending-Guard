package com.joajy.spendingguard.analysis.domain.model;

/**
 * 소비 알림이 나타내는 금액 변화의 종류다.
 *
 * <p>취소와 환불을 결제와 구분해야 이후 원장에서 반대 부호로 반영할 수 있다.
 */
public enum TransactionType {
    PAYMENT,
    CANCEL,
    REFUND
}
