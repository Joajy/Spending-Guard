package com.joajy.spendingguard.spendevent.controller.dto.response;

import java.util.List;

import com.joajy.spendingguard.spendevent.service.result.SpendEventHistoryPage;

public record SpendEventHistoryResponse(
        List<SpendEventHistoryItemResponse> items,
        String nextCursor,
        boolean hasNext
) {
    public static SpendEventHistoryResponse from(SpendEventHistoryPage page) {
        return new SpendEventHistoryResponse(
                page.items().stream().map(SpendEventHistoryItemResponse::from).toList(),
                page.nextCursor(),
                page.hasNext()
        );
    }
}
