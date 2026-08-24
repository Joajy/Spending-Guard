package com.joajy.spendingguard.analysis.service.port.inbound;

import com.joajy.spendingguard.analysis.service.command.CorrectSpendCategoryCommand;
import com.joajy.spendingguard.analysis.service.result.SpendCategoryCorrectionResult;

/** 사용자가 자동 분류된 소비 카테고리를 수정하는 입력 포트다. */
public interface CorrectSpendCategoryUseCase {
    SpendCategoryCorrectionResult correct(CorrectSpendCategoryCommand command);
}
