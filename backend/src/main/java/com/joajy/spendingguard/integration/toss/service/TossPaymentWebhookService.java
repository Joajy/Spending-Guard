package com.joajy.spendingguard.integration.toss.service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.joajy.spendingguard.integration.toss.client.TossPaymentClient;
import com.joajy.spendingguard.integration.toss.client.TossPaymentClient.Cancel;
import com.joajy.spendingguard.integration.toss.client.TossPaymentClient.Payment;
import com.joajy.spendingguard.integration.toss.config.TossPaymentProperties;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.service.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.service.exception.DuplicateSpendEventException;
import com.joajy.spendingguard.spendevent.service.port.inbound.SubmitSpendEventUseCase;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 검증된 Toss 승인·취소를 기존 소비 이벤트 파이프라인으로 전달한다. */
@Service
public class TossPaymentWebhookService {

    static final String PAYMENT_STATUS_CHANGED = "PAYMENT_STATUS_CHANGED";
    private static final String COMPLETED_CANCEL_STATUS = "DONE";

    private final TossPaymentClient tossPaymentClient;
    private final TossPaymentProperties properties;
    private final SubmitSpendEventUseCase submitSpendEventUseCase;

    public TossPaymentWebhookService(
            TossPaymentClient tossPaymentClient,
            TossPaymentProperties properties,
            SubmitSpendEventUseCase submitSpendEventUseCase
    ) {
        this.tossPaymentClient = tossPaymentClient;
        this.properties = properties;
        this.submitSpendEventUseCase = submitSpendEventUseCase;
    }

    public Result process(String eventType, String webhookPaymentKey) {
        if (!PAYMENT_STATUS_CHANGED.equals(eventType)) {
            return Result.ignoredResult();
        }

        UUID userId = properties.configuredUserId();
        Payment payment = tossPaymentClient.getPayment(webhookPaymentKey);
        verifyPayment(webhookPaymentKey, payment);

        Counter counter = new Counter();
        if (payment.approvedAt() != null && payment.totalAmount() > 0) {
            counter.record(submit(
                    userId,
                    payment.paymentKey(),
                    payment.orderName(),
                    payment.totalAmount(),
                    "결제",
                    payment.approvedAt().toInstant()
            ));
        }
        for (Cancel cancel : payment.cancels()) {
            if (isCompleted(cancel)) {
                counter.record(submit(
                        userId,
                        "cancel:" + cancel.transactionKey(),
                        payment.orderName(),
                        cancel.cancelAmount(),
                        "결제 취소",
                        cancel.canceledAt().toInstant()
                ));
            }
        }
        return new Result(false, counter.accepted, counter.duplicates);
    }

    private void verifyPayment(String webhookPaymentKey, Payment payment) {
        if (!Objects.equals(webhookPaymentKey, payment.paymentKey())) {
            throw new TossPaymentVerificationException("웹훅과 조회 결제의 paymentKey가 다릅니다.");
        }
        if (!Objects.equals(properties.merchantId(), payment.mId())) {
            throw new TossPaymentVerificationException("설정된 상점의 결제가 아닙니다.");
        }
    }

    private boolean isCompleted(Cancel cancel) {
        return cancel != null
                && COMPLETED_CANCEL_STATUS.equals(cancel.cancelStatus())
                && cancel.cancelAmount() > 0
                && cancel.canceledAt() != null
                && StringUtils.hasText(cancel.transactionKey());
    }

    private boolean submit(
            UUID userId,
            String externalEventId,
            String orderName,
            long amount,
            String action,
            Instant occurredAt
    ) {
        String normalizedOrderName = StringUtils.hasText(orderName)
                ? orderName.trim().replaceAll("\\s+", " ")
                : "토스페이먼츠 주문";
        try {
            submitSpendEventUseCase.submit(new SubmitSpendEventCommand(
                    userId,
                    SpendEventSource.TOSS_WEBHOOK,
                    externalEventId,
                    normalizedOrderName + " " + amount + "원 " + action,
                    occurredAt
            ));
            return true;
        } catch (DuplicateSpendEventException exception) {
            return false;
        }
    }

    public record Result(boolean ignored, int acceptedCount, int duplicateCount) {
        static Result ignoredResult() {
            return new Result(true, 0, 0);
        }
    }

    private static final class Counter {
        private int accepted;
        private int duplicates;

        private void record(boolean wasAccepted) {
            if (wasAccepted) {
                accepted++;
            } else {
                duplicates++;
            }
        }
    }
}
