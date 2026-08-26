package com.joajy.spendingguard.riskalert.service.result;

import java.util.List;

public record RiskAlertPage(
        List<RiskAlertItem> items,
        String nextCursor,
        boolean hasNext
) {
}
