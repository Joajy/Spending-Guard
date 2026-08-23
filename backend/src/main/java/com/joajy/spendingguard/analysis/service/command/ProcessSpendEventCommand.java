package com.joajy.spendingguard.analysis.service.command;

import java.util.Objects;
import java.util.UUID;

/** @param eventId 분석할 원천 소비 이벤트 식별자 */
public record ProcessSpendEventCommand(UUID eventId) {

    public ProcessSpendEventCommand {
        Objects.requireNonNull(eventId, "소비 이벤트 식별자가 필요합니다.");
    }
}
