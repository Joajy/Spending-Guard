package com.joajy.spendingguard.analysis.domain.model;

/** 소비 알림의 필수 필드를 규칙만으로 추출할 수 있었는지 나타낸다. */
public enum FastParseStatus {
    PARSED,
    NEEDS_REVIEW
}
