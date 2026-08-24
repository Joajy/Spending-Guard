package com.joajy.spendingguard.analysis.controller;

import java.util.UUID;

import com.joajy.spendingguard.analysis.controller.dto.request.CorrectSpendCategoryRequest;
import com.joajy.spendingguard.analysis.controller.dto.response.SpendCategoryCorrectionResponse;
import com.joajy.spendingguard.analysis.service.command.CorrectSpendCategoryCommand;
import com.joajy.spendingguard.analysis.service.port.inbound.CorrectSpendCategoryUseCase;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 분석이 끝난 소비 내역의 카테고리를 사용자가 바로잡는 HTTP 입력 어댑터다. */
@RestController
@RequestMapping("/api/v1/users/{userId}/spend-events/{eventId}/category")
public class SpendCategoryCorrectionController {

    private final CorrectSpendCategoryUseCase useCase;

    public SpendCategoryCorrectionController(CorrectSpendCategoryUseCase useCase) {
        this.useCase = useCase;
    }

    @PatchMapping
    @PreAuthorize("@userScope.matches(authentication, #userId)")
    public SpendCategoryCorrectionResponse correct(
            @PathVariable UUID userId,
            @PathVariable UUID eventId,
            @Valid @RequestBody CorrectSpendCategoryRequest request
    ) {
        return SpendCategoryCorrectionResponse.from(useCase.correct(
                new CorrectSpendCategoryCommand(
                        userId, eventId, request.category(), request.expectedVersion()
                )
        ));
    }
}
