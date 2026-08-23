package com.joajy.spendingguard.analysis.domain.policy;

import java.math.BigDecimal;
import java.time.Instant;

import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.analysis.domain.model.SpendCategory;
import com.joajy.spendingguard.analysis.domain.model.TransactionType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.assertj.core.api.Assertions.assertThat;

/** 고정된 회귀 데이터셋으로 카테고리와 위험도 판정 정확도를 재현한다. */
class SpendRiskClassifierEvaluationTest {

    private final SpendRiskClassifier classifier = new SpendRiskClassifier();

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvFileSource(resources = "/analysis/risk-classifier-evaluation.csv", numLinesToSkip = 1)
    void matchesExpectedAssessment(
            String message,
            String amount,
            TransactionType transactionType,
            String occurredAt,
            SpendCategory category,
            boolean fixedCost,
            RiskLevel riskLevel,
            String reasonCode
    ) {
        var result = classifier.classify(
                message, new BigDecimal(amount), transactionType, Instant.parse(occurredAt)
        );

        assertThat(result.category()).isEqualTo(category);
        assertThat(result.fixedCost()).isEqualTo(fixedCost);
        assertThat(result.riskLevel()).isEqualTo(riskLevel);
        assertThat(result.reasonCode()).isEqualTo(reasonCode);
    }
}
