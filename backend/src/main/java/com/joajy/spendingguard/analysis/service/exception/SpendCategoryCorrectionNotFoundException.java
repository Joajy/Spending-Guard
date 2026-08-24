package com.joajy.spendingguard.analysis.service.exception;

import java.util.UUID;

public class SpendCategoryCorrectionNotFoundException extends RuntimeException {
    public SpendCategoryCorrectionNotFoundException(UUID eventId) {
        super("수정할 소비 내역을 찾을 수 없습니다: " + eventId);
    }
}
