package com.joajy.spendingguard.riskalert.service.port;

import java.util.List;
import java.util.UUID;

import com.joajy.spendingguard.riskalert.service.model.RiskAlertQuery;
import com.joajy.spendingguard.riskalert.service.result.RiskAlertItem;

/** 위험 알림 피드에 필요한 사용자 확인과 정렬된 항목 조회를 제공한다. */
public interface RiskAlertQueryPort {

    boolean userExists(UUID userId);

    List<RiskAlertItem> find(RiskAlertQuery query);
}
