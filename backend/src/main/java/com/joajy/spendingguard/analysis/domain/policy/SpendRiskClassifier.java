package com.joajy.spendingguard.analysis.domain.policy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.analysis.domain.model.SpendCategory;
import com.joajy.spendingguard.analysis.domain.model.SpendRiskAssessment;
import com.joajy.spendingguard.analysis.domain.model.TransactionType;

/** 금액·시각·상호 키워드로 재현 가능한 소비 위험 신호를 만든다. */
public class SpendRiskClassifier {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final BigDecimal HIGH_AMOUNT = BigDecimal.valueOf(500_000);
    private static final BigDecimal MEDIUM_AMOUNT = BigDecimal.valueOf(200_000);
    private static final BigDecimal LATE_NIGHT_TRANSPORT_AMOUNT = BigDecimal.valueOf(20_000);
    private static final BigDecimal DELIVERY_AMOUNT = BigDecimal.valueOf(50_000);

    public SpendRiskAssessment classify(
            String message,
            BigDecimal amount,
            TransactionType transactionType,
            Instant occurredAt
    ) {
        SpendCategory category = category(message);
        boolean fixedCost = category == SpendCategory.SUBSCRIPTION;
        if (transactionType != TransactionType.PAYMENT) {
            return new SpendRiskAssessment(category, fixedCost, RiskLevel.LOW, "NON_PAYMENT");
        }
        if (amount.compareTo(HIGH_AMOUNT) >= 0) {
            return new SpendRiskAssessment(category, fixedCost, RiskLevel.HIGH, "LARGE_PAYMENT");
        }
        if (category == SpendCategory.TRANSPORT
                && isLateNight(occurredAt)
                && amount.compareTo(LATE_NIGHT_TRANSPORT_AMOUNT) >= 0) {
            return new SpendRiskAssessment(
                    category, fixedCost, RiskLevel.HIGH, "LATE_NIGHT_TRANSPORT"
            );
        }
        if (amount.compareTo(MEDIUM_AMOUNT) >= 0) {
            return new SpendRiskAssessment(category, fixedCost, RiskLevel.MEDIUM, "ELEVATED_AMOUNT");
        }
        if (category == SpendCategory.DELIVERY && amount.compareTo(DELIVERY_AMOUNT) >= 0) {
            return new SpendRiskAssessment(
                    category, fixedCost, RiskLevel.MEDIUM, "HIGH_DELIVERY_AMOUNT"
            );
        }
        return new SpendRiskAssessment(category, fixedCost, RiskLevel.LOW, "NORMAL_PATTERN");
    }

    private SpendCategory category(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "구독", "멤버십", "넷플릭스", "netflix", "유튜브 프리미엄", "와우")) {
            return SpendCategory.SUBSCRIPTION;
        }
        if (containsAny(normalized, "카카오t", "택시", "버스", "지하철", "철도")) {
            return SpendCategory.TRANSPORT;
        }
        if (containsAny(normalized, "배달의민족", "배민", "요기요", "쿠팡이츠", "배달")) {
            return SpendCategory.DELIVERY;
        }
        if (containsAny(normalized, "쿠팡", "쇼핑", "스토어", "마켓", "백화점")) {
            return SpendCategory.SHOPPING;
        }
        if (containsAny(normalized, "송금", "이체")) {
            return SpendCategory.TRANSFER;
        }
        return SpendCategory.OTHER;
    }

    private boolean isLateNight(Instant occurredAt) {
        if (occurredAt == null) {
            return false;
        }
        int hour = occurredAt.atZone(SERVICE_ZONE).getHour();
        return hour < 5;
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
