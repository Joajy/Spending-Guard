package com.joajy.spendingguard.analysis.controller.dto.request;

import com.joajy.spendingguard.analysis.domain.model.SpendCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CorrectSpendCategoryRequest(
        @NotNull SpendCategory category,
        @NotNull @PositiveOrZero Long expectedVersion
) {
}
