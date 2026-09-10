package com.joajy.spendingguard.integration.toss.client;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 웹훅에 포함된 paymentKey를 Toss Payments 원본 결제와 대조하는 출력 포트다. */
public interface TossPaymentClient {

    Payment getPayment(String paymentKey);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Payment(
            String mId,
            String paymentKey,
            String orderId,
            String orderName,
            long totalAmount,
            String status,
            OffsetDateTime approvedAt,
            List<Cancel> cancels
    ) {
        public Payment {
            cancels = cancels == null ? List.of() : List.copyOf(cancels);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Cancel(
            long cancelAmount,
            String cancelStatus,
            OffsetDateTime canceledAt,
            String transactionKey
    ) {
    }
}
