package com.joajy.spendingguard.integration.toss.service;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.integration.toss.client.TossPaymentClient;
import com.joajy.spendingguard.integration.toss.client.TossPaymentClient.Cancel;
import com.joajy.spendingguard.integration.toss.client.TossPaymentClient.Payment;
import com.joajy.spendingguard.integration.toss.config.TossPaymentProperties;
import com.joajy.spendingguard.integration.toss.service.model.TossTestOrder;
import com.joajy.spendingguard.integration.toss.service.port.outbound.TossTestOrderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TossTestCheckoutServiceTest {

    private static final UUID USER_ID = UUID.fromString("cc9a3956-9604-460b-9af4-435d5f892cc4");
    private static final Instant NOW = Instant.parse("2026-09-10T05:00:00Z");

    @Mock private TossPaymentClient paymentClient;
    @Mock private TossTestOrderPort orderPort;
    @Mock private TossPaymentWebhookService webhookService;

    private TossTestCheckoutService service;

    @BeforeEach
    void setUp() {
        TossPaymentProperties properties = new TossPaymentProperties(
                true,
                URI.create("https://api.tosspayments.com"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                "test_ck",
                "test_sk",
                "test-mid",
                USER_ID.toString()
        );
        service = new TossTestCheckoutService(
                paymentClient,
                properties,
                orderPort,
                webhookService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsServerOwnedOrderBeforeOpeningPaymentWindow() {
        TossTestCheckoutService.CreatedOrder result = service.create(
                USER_ID,
                12_800,
                "  Spending   Guard 테스트  "
        );

        assertThat(result.clientKey()).isEqualTo("test_ck");
        assertThat(result.amount()).isEqualTo(12_800);
        assertThat(result.orderName()).isEqualTo("Spending Guard 테스트");
        assertThat(result.orderId()).startsWith("sg_").hasSize(35);
        ArgumentCaptor<TossTestOrder> order = ArgumentCaptor.forClass(TossTestOrder.class);
        verify(orderPort).create(order.capture());
        assertThat(order.getValue().createdAt()).isEqualTo(NOW);
    }

    @Test
    void confirmsOnlyClaimedOrderAndImmediatelySubmitsVerifiedPayment() {
        TossTestOrder order = new TossTestOrder("sg_order123", USER_ID, 12_800, "테스트 주문", NOW);
        Payment payment = payment("sg_order123", 12_800);
        given(orderPort.claimForConfirmation(USER_ID, "sg_order123", 12_800, NOW))
                .willReturn(Optional.of(order));
        given(paymentClient.confirmPayment("payment-key", "sg_order123", 12_800))
                .willReturn(payment);
        given(webhookService.processVerifiedPayment(USER_ID, payment))
                .willReturn(new TossPaymentWebhookService.Result(false, 1, 0));

        TossTestCheckoutService.ConfirmedOrder result = service.confirm(
                USER_ID,
                "sg_order123",
                "payment-key",
                12_800
        );

        assertThat(result.status()).isEqualTo("DONE");
        assertThat(result.acceptedCount()).isOne();
        verify(orderPort).markConfirmed("sg_order123", "payment-key", NOW);
        verify(webhookService).processVerifiedPayment(USER_ID, payment);
    }

    @Test
    void releasesClaimWhenTossConfirmationFails() {
        TossTestOrder order = new TossTestOrder("sg_order123", USER_ID, 12_800, "테스트 주문", NOW);
        given(orderPort.claimForConfirmation(USER_ID, "sg_order123", 12_800, NOW))
                .willReturn(Optional.of(order));
        given(paymentClient.confirmPayment(any(), any(), anyLong()))
                .willThrow(new TossPaymentVerificationException("승인 실패"));

        assertThatThrownBy(() -> service.confirm(USER_ID, "sg_order123", "payment-key", 12_800))
                .isInstanceOf(TossPaymentVerificationException.class);
        verify(orderPort).releaseConfirmation("sg_order123");
    }

    @Test
    void rejectsAmountChangedAfterOrderCreation() {
        given(orderPort.claimForConfirmation(USER_ID, "sg_order123", 100, NOW))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(USER_ID, "sg_order123", "payment-key", 100))
                .isInstanceOf(TossTestOrderStateException.class);
    }

    @Test
    void cancelsConfirmedOrderAndSubmitsCancellation() {
        TossTestOrder order = new TossTestOrder("sg_order123", USER_ID, 12_800, "테스트 주문", NOW);
        Payment canceled = new Payment(
                "test-mid",
                "payment-key",
                "sg_order123",
                "테스트 주문",
                12_800,
                "CANCELED",
                OffsetDateTime.parse("2026-09-10T14:00:00+09:00"),
                List.of(new Cancel(
                        12_800,
                        "DONE",
                        OffsetDateTime.parse("2026-09-10T14:05:00+09:00"),
                        "cancel-key"
                ))
        );
        given(orderPort.claimForCancellation(USER_ID, "sg_order123", "payment-key", NOW))
                .willReturn(Optional.of(order));
        given(paymentClient.cancelPayment("payment-key", "Spending Guard 자동수집 취소 시연"))
                .willReturn(canceled);
        given(webhookService.processVerifiedPayment(USER_ID, canceled))
                .willReturn(new TossPaymentWebhookService.Result(false, 1, 1));

        TossTestCheckoutService.CanceledOrder result = service.cancel(
                USER_ID,
                "sg_order123",
                "payment-key"
        );

        assertThat(result.status()).isEqualTo("CANCELED");
        assertThat(result.acceptedCount()).isOne();
        assertThat(result.duplicateCount()).isOne();
        verify(orderPort).markCanceled("sg_order123", NOW);
    }

    private Payment payment(String orderId, long amount) {
        return new Payment(
                "test-mid",
                "payment-key",
                orderId,
                "테스트 주문",
                amount,
                "DONE",
                OffsetDateTime.parse("2026-09-10T14:00:00+09:00"),
                List.of()
        );
    }
}
