package com.joajy.spendingguard.spendevent.application;

public class DuplicateSpendEventException extends RuntimeException {

    public DuplicateSpendEventException() {
        super("이미 접수된 소비 알림입니다.");
    }
}
