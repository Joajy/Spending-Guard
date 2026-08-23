package com.joajy.spendingguard.analysis.service.result;

/** 소비 이벤트 한 건에 대한 빠른 분석 유스케이스의 처리 결과다. */
public enum SpendEventProcessingResult {
    PROCESSED,
    NEEDS_REVIEW,
    ALREADY_PROCESSED
}
