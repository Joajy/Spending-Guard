package com.joajy.spendingguard.analysis.service.command;

import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.SpendCategory;

/** 사용자 분류 수정과 화면이 조회한 현재 버전을 전달하는 명령이다. */
public record CorrectSpendCategoryCommand(
        UUID userId,
        UUID eventId,
        SpendCategory category,
        long expectedVersion
) {
}
