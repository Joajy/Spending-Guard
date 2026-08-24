package com.joajy.spendingguard.analysis.service.exception;

public class SpendCategoryNotEditableException extends RuntimeException {
    public SpendCategoryNotEditableException() {
        super("분석이 완료된 소비 내역만 카테고리를 수정할 수 있습니다.");
    }
}
