package com.joajy.spendingguard.analysis.domain.policy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.joajy.spendingguard.analysis.domain.model.FastParseOutcome;
import com.joajy.spendingguard.analysis.domain.model.TransactionType;

/**
 * 금융 알림 텍스트에서 원화 금액과 거래유형을 즉시 추출하는 결정론적 파서다.
 *
 * <p>LLM 호출보다 먼저 실행되어 외부 서비스 장애 중에도 최소한의 구조화 결과를 남긴다.
 * 금액은 {@code 원} 단위가 붙은 첫 값을 사용하고, 환불·취소·결제 순서로 키워드를
 * 판정한다. 모호한 입력은 임의로 추측하지 않고 검토 대상으로 반환한다.
 */
public class FastSpendEventParser {

    private static final Pattern WON_AMOUNT = Pattern.compile(
            "(?<!\\d)(\\d[\\d,]*(?:\\.\\d{1,2})?)\\s*원"
    );
    private static final Pattern REFUND = Pattern.compile("환불|반품");
    private static final Pattern CANCEL = Pattern.compile("취소|승인취소");
    private static final Pattern PAYMENT = Pattern.compile("결제|승인|사용|출금|송금|이체");

    public FastParseOutcome parse(String message) {
        if (message == null || message.isBlank()) {
            return FastParseOutcome.needsReview(
                    null,
                    null,
                    "EMPTY_MESSAGE"
            );
        }

        BigDecimal amount = extractAmount(message);
        TransactionType transactionType = extractTransactionType(message);
        List<String> issues = new ArrayList<>();
        if (amount == null) {
            issues.add("AMOUNT_NOT_FOUND");
        }
        if (transactionType == null) {
            issues.add("TRANSACTION_TYPE_NOT_FOUND");
        }

        if (!issues.isEmpty()) {
            return FastParseOutcome.needsReview(
                    amount,
                    transactionType,
                    String.join(",", issues)
            );
        }
        return FastParseOutcome.parsed(amount, transactionType);
    }

    private BigDecimal extractAmount(String message) {
        Matcher matcher = WON_AMOUNT.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(matcher.group(1).replace(",", ""));
            return amount.signum() > 0 ? amount : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private TransactionType extractTransactionType(String message) {
        if (REFUND.matcher(message).find()) {
            return TransactionType.REFUND;
        }
        if (CANCEL.matcher(message).find()) {
            return TransactionType.CANCEL;
        }
        if (PAYMENT.matcher(message).find()) {
            return TransactionType.PAYMENT;
        }
        return null;
    }
}
