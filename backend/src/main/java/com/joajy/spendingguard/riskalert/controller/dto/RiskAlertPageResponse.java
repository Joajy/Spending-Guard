package com.joajy.spendingguard.riskalert.controller.dto;

import java.util.List;

import com.joajy.spendingguard.riskalert.service.result.RiskAlertPage;

public record RiskAlertPageResponse(
        List<RiskAlertItemResponse> items,
        String nextCursor,
        boolean hasNext
) {

    public static RiskAlertPageResponse from(RiskAlertPage page) {
        return new RiskAlertPageResponse(
                page.items().stream().map(RiskAlertItemResponse::from).toList(),
                page.nextCursor(),
                page.hasNext()
        );
    }
}
