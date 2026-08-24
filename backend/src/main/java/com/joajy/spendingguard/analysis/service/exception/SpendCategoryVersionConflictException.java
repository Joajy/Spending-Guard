package com.joajy.spendingguard.analysis.service.exception;

public class SpendCategoryVersionConflictException extends RuntimeException {
    public SpendCategoryVersionConflictException() {
        super("다른 요청이 먼저 카테고리를 수정했습니다. 최신 내역을 다시 조회해 주세요.");
    }
}
