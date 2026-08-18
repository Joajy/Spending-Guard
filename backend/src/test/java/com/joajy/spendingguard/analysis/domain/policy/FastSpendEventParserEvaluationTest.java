package com.joajy.spendingguard.analysis.domain.policy;

import com.joajy.spendingguard.analysis.domain.model.FastParseStatus;
import com.joajy.spendingguard.analysis.domain.model.TransactionType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.assertj.core.api.Assertions.assertThat;

/** 공개된 회귀 데이터셋에서 금액과 거래유형의 정확도를 재현한다. */
class FastSpendEventParserEvaluationTest {

    private final FastSpendEventParser parser = new FastSpendEventParser();

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvFileSource(
            resources = "/analysis/fast-parser-evaluation.csv",
            numLinesToSkip = 1
    )
    void matchesExpectedAmountAndTransactionType(
            String message,
            String expectedAmount,
            TransactionType expectedType
    ) {
        var outcome = parser.parse(message);

        assertThat(outcome.status()).isEqualTo(FastParseStatus.PARSED);
        assertThat(outcome.amount()).isEqualByComparingTo(expectedAmount);
        assertThat(outcome.transactionType()).isEqualTo(expectedType);
    }
}
