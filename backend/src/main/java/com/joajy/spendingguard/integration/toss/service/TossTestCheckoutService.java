package com.joajy.spendingguard.integration.toss.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.joajy.spendingguard.integration.toss.client.TossPaymentClient;
import com.joajy.spendingguard.integration.toss.client.TossPaymentClient.Payment;
import com.joajy.spendingguard.integration.toss.config.TossPaymentProperties;
import com.joajy.spendingguard.integration.toss.service.model.TossTestOrder;
import com.joajy.spendingguard.integration.toss.service.port.outbound.TossTestOrderPort;
import org.springframework.stereotype.Service;

/** 인증된 사용자를 위한 Toss 테스트 주문 생성·승인 유스케이스다. */
@Service
public class TossTestCheckoutService {

    public static final long MINIMUM_AMOUNT = 100L;
    public static final long MAXIMUM_AMOUNT = 1_000_000L;
    private static final String CONFIRMED_STATUS = "DONE";
    private static final String CANCELED_STATUS = "CANCELED";
    private static final String TEST_CANCEL_REASON = "Spending Guard 자동수집 취소 시연";
    private static final String ORDER_ID_PREFIX = "sg_";

    private final TossPaymentClient paymentClient;
    private final TossPaymentProperties properties;
    private final TossTestOrderPort orderPort;
    private final TossPaymentWebhookService webhookService;
    private final Clock clock;

    public TossTestCheckoutService(
            TossPaymentClient paymentClient,
            TossPaymentProperties properties,
            TossTestOrderPort orderPort,
            TossPaymentWebhookService webhookService,
            Clock clock
    ) {
        this.paymentClient = paymentClient;
        this.properties = properties;
        this.orderPort = orderPort;
        this.webhookService = webhookService;
        this.clock = clock;
    }

    public CreatedOrder create(UUID userId, long amount, String orderName) {
        verifyConnectedUser(userId);
        if (amount < MINIMUM_AMOUNT || amount > MAXIMUM_AMOUNT) {
            throw new IllegalArgumentException("테스트 결제 금액은 100원 이상 1,000,000원 이하여야 합니다.");
        }
        String normalizedName = normalizeOrderName(orderName);
        Instant createdAt = clock.instant();
        TossTestOrder order = new TossTestOrder(
                ORDER_ID_PREFIX + UUID.randomUUID().toString().replace("-", ""),
                userId,
                amount,
                normalizedName,
                createdAt
        );
        orderPort.create(order);
        return new CreatedOrder(
                order.orderId(),
                properties.configuredClientKey(),
                order.amount(),
                order.orderName()
        );
    }

    public ConfirmedOrder confirm(UUID userId, String orderId, String paymentKey, long amount) {
        verifyConnectedUser(userId);
        Instant now = clock.instant();
        TossTestOrder order = orderPort.claimForConfirmation(userId, orderId, amount, now)
                .orElseThrow(TossTestOrderStateException::new);
        Payment payment;
        try {
            payment = paymentClient.confirmPayment(paymentKey, order.orderId(), order.amount());
            verifyConfirmation(order, paymentKey, payment);
        } catch (TossPaymentVerificationException exception) {
            orderPort.releaseConfirmation(orderId);
            throw exception;
        }

        orderPort.markConfirmed(orderId, payment.paymentKey(), now);
        TossPaymentWebhookService.Result ingestion = webhookService.processVerifiedPayment(userId, payment);
        return new ConfirmedOrder(
                payment.paymentKey(),
                payment.orderId(),
                payment.status(),
                ingestion.acceptedCount(),
                ingestion.duplicateCount()
        );
    }

    public CanceledOrder cancel(UUID userId, String orderId, String paymentKey) {
        verifyConnectedUser(userId);
        Instant canceledAt = clock.instant();
        TossTestOrder order = orderPort.claimForCancellation(
                        userId,
                        orderId,
                        paymentKey,
                        canceledAt
                )
                .orElseThrow(TossTestOrderStateException::new);
        Payment payment;
        try {
            payment = paymentClient.cancelPayment(paymentKey, TEST_CANCEL_REASON);
            if (!Objects.equals(order.orderId(), payment.orderId())
                    || !Objects.equals(paymentKey, payment.paymentKey())
                    || !Objects.equals(properties.merchantId(), payment.mId())
                    || !CANCELED_STATUS.equals(payment.status())) {
                throw new TossPaymentVerificationException("취소된 결제 정보가 저장된 테스트 주문과 다릅니다.");
            }
        } catch (TossPaymentVerificationException exception) {
            orderPort.releaseCancellation(orderId);
            throw exception;
        }
        orderPort.markCanceled(orderId, canceledAt);
        TossPaymentWebhookService.Result ingestion = webhookService.processVerifiedPayment(userId, payment);
        return new CanceledOrder(
                payment.paymentKey(),
                payment.orderId(),
                payment.status(),
                ingestion.acceptedCount(),
                ingestion.duplicateCount()
        );
    }

    private void verifyConnectedUser(UUID userId) {
        if (!Objects.equals(properties.configuredUserId(), userId)) {
            throw new IllegalArgumentException("현재 Toss 테스트 상점에 연결된 사용자가 아닙니다.");
        }
    }

    private void verifyConfirmation(TossTestOrder order, String paymentKey, Payment payment) {
        if (!Objects.equals(paymentKey, payment.paymentKey())
                || !Objects.equals(order.orderId(), payment.orderId())
                || order.amount() != payment.totalAmount()
                || !Objects.equals(properties.merchantId(), payment.mId())
                || !CONFIRMED_STATUS.equals(payment.status())) {
            throw new TossPaymentVerificationException("승인된 결제 정보가 저장된 테스트 주문과 다릅니다.");
        }
    }

    private String normalizeOrderName(String orderName) {
        if (orderName == null || orderName.isBlank()) {
            return "Spending Guard 테스트 결제";
        }
        String normalized = orderName.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("테스트 주문명은 100자 이하여야 합니다.");
        }
        return normalized;
    }

    public record CreatedOrder(String orderId, String clientKey, long amount, String orderName) {
    }

    public record ConfirmedOrder(
            String paymentKey,
            String orderId,
            String status,
            int acceptedCount,
            int duplicateCount
    ) {
    }

    public record CanceledOrder(
            String paymentKey,
            String orderId,
            String status,
            int acceptedCount,
            int duplicateCount
    ) {
    }
}
