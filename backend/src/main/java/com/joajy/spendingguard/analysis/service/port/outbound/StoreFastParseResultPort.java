package com.joajy.spendingguard.analysis.service.port.outbound;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.FastParseOutcome;

/** 빠른 파싱 결과를 원천 이벤트와 일대일로 저장하는 출력 포트다. */
public interface StoreFastParseResultPort {

    void store(UUID eventId, FastParseOutcome outcome, String parserVersion, Instant parsedAt);
}
