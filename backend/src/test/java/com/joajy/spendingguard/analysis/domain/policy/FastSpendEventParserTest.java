package com.joajy.spendingguard.analysis.domain.policy;

import com.joajy.spendingguard.analysis.domain.model.FastParseStatus;
import com.joajy.spendingguard.analysis.domain.model.TransactionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FastSpendEventParserTest {

    private final FastSpendEventParser parser = new FastSpendEventParser();

    @Test
    void prioritizesRefundOverPaymentKeyword() {
        var outcome = parser.parse("쿠팡 결제 12,800원 전액 환불");

        assertThat(outcome.status()).isEqualTo(FastParseStatus.PARSED);
        assertThat(outcome.transactionType()).isEqualTo(TransactionType.REFUND);
    }

    @Test
    void prioritizesCancellationOverApprovalKeyword() {
        var outcome = parser.parse("국민카드 승인취소 28,000원 카카오T");

        assertThat(outcome.status()).isEqualTo(FastParseStatus.PARSED);
        assertThat(outcome.transactionType()).isEqualTo(TransactionType.CANCEL);
    }

    @Test
    void returnsReviewReasonWhenAmountIsMissing() {
        var outcome = parser.parse("배달의민족 결제 완료");

        assertThat(outcome.status()).isEqualTo(FastParseStatus.NEEDS_REVIEW);
        assertThat(outcome.amount()).isNull();
        assertThat(outcome.transactionType()).isEqualTo(TransactionType.PAYMENT);
        assertThat(outcome.reviewReason()).isEqualTo("AMOUNT_NOT_FOUND");
    }

    @Test
    void returnsReviewReasonWhenTransactionTypeIsMissing() {
        var outcome = parser.parse("넷플릭스 17,000원");

        assertThat(outcome.status()).isEqualTo(FastParseStatus.NEEDS_REVIEW);
        assertThat(outcome.amount()).isEqualByComparingTo("17000");
        assertThat(outcome.transactionType()).isNull();
        assertThat(outcome.reviewReason()).isEqualTo("TRANSACTION_TYPE_NOT_FOUND");
    }

    @Test
    void doesNotTreatZeroWonAsAValidAmount() {
        var outcome = parser.parse("테스트가맹점 0원 승인");

        assertThat(outcome.status()).isEqualTo(FastParseStatus.NEEDS_REVIEW);
        assertThat(outcome.reviewReason()).isEqualTo("AMOUNT_NOT_FOUND");
    }

    @Test
    void returnsStableReasonForBlankMessage() {
        var outcome = parser.parse("   ");

        assertThat(outcome.status()).isEqualTo(FastParseStatus.NEEDS_REVIEW);
        assertThat(outcome.reviewReason()).isEqualTo("EMPTY_MESSAGE");
    }
}
