package com.joajy.spendingguard.spendevent;

public class DuplicateSpendEventException extends RuntimeException {

    public DuplicateSpendEventException() {
        super("이미 접수된 소비 알림입니다.");
    }
}
