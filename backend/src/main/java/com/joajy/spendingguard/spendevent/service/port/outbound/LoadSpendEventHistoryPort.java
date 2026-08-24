package com.joajy.spendingguard.spendevent.service.port.outbound;

import java.util.List;

import com.joajy.spendingguard.spendevent.service.model.SpendEventHistoryQuery;
import com.joajy.spendingguard.spendevent.service.result.SpendEventHistoryItem;

/** 저장소에서 정렬·필터·페이지 제한이 적용된 소비 내역을 읽는 출력 포트다. */
public interface LoadSpendEventHistoryPort {
    List<SpendEventHistoryItem> find(SpendEventHistoryQuery query);
}
