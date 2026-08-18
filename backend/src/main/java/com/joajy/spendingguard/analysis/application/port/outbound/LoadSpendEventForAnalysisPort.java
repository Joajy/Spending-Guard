package com.joajy.spendingguard.analysis.application.port.outbound;

import java.util.UUID;

import com.joajy.spendingguard.analysis.application.model.SpendEventAnalysisTarget;

/** 원천 저장소에서 빠른 분석에 필요한 최소 데이터를 읽는 출력 포트다. */
public interface LoadSpendEventForAnalysisPort {

    SpendEventAnalysisTarget load(UUID eventId);
}
