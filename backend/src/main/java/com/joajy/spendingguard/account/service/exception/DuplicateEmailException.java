package com.joajy.spendingguard.account.service.exception;

/** 정규화된 이메일이 이미 등록되어 있을 때 반환하는 업무 예외다. */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("이미 등록된 이메일입니다.");
    }
}
