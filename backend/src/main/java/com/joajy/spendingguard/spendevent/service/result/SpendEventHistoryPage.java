package com.joajy.spendingguard.spendevent.service.result;

import java.util.List;

public record SpendEventHistoryPage(
        List<SpendEventHistoryItem> items,
        String nextCursor,
        boolean hasNext
) {
    public SpendEventHistoryPage {
        items = List.copyOf(items);
    }
}
