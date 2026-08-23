package com.joajy.spendingguard.analysis.domain.policy;

import java.math.BigDecimal;
import java.time.Instant;

import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.analysis.domain.model.SpendCategory;
import com.joajy.spendingguard.analysis.domain.model.TransactionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpendRiskClassifierTest {

    private final SpendRiskClassifier classifier = new SpendRiskClassifier();

    @Test
    void marksLateNightTaxiAsHighRisk() {
        var result = classifier.classify(
                "새벽 카카오T 28,000원 결제",
                new BigDecimal("28000"),
                TransactionType.PAYMENT,
                Instant.parse("2026-08-22T17:00:00Z")
        );

        assertThat(result.category()).isEqualTo(SpendCategory.TRANSPORT);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.reasonCode()).isEqualTo("LATE_NIGHT_TRANSPORT");
    }

    @Test
    void recognizesMembershipAsFixedSubscription() {
        var result = classifier.classify(
                "쿠팡 와우 멤버십 4,900원 결제",
                new BigDecimal("4900"),
                TransactionType.PAYMENT,
                Instant.parse("2026-08-23T03:00:00Z")
        );

        assertThat(result.category()).isEqualTo(SpendCategory.SUBSCRIPTION);
        assertThat(result.fixedCost()).isTrue();
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void marksLargePaymentAsHighRisk() {
        var result = classifier.classify(
                "전자제품 850,000원 결제",
                new BigDecimal("850000"),
                TransactionType.PAYMENT,
                Instant.parse("2026-08-23T03:00:00Z")
        );

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.reasonCode()).isEqualTo("LARGE_PAYMENT");
    }

    @Test
    void doesNotRaiseSpendRiskForRefund() {
        var result = classifier.classify(
                "쿠팡 850,000원 환불",
                new BigDecimal("850000"),
                TransactionType.REFUND,
                Instant.parse("2026-08-23T03:00:00Z")
        );

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.reasonCode()).isEqualTo("NON_PAYMENT");
    }
}
