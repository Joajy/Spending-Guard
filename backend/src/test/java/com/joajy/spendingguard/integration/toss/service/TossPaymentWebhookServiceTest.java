package com.joajy.spendingguard.integration.toss.service;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.joajy.spendingguard.integration.toss.client.TossPaymentClient;
import com.joajy.spendingguard.integration.toss.client.TossPaymentClient.Cancel;
import com.joajy.spendingguard.integration.toss.client.TossPaymentClient.Payment;
import com.joajy.spendingguard.integration.toss.config.TossPaymentProperties;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.service.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.service.exception.DuplicateSpendEventException;
import com.joajy.spendingguard.spendevent.service.port.inbound.SubmitSpendEventUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TossPaymentWebhookServiceTest {

    private static final UUID USER_ID = UUID.fromString("72b92902-aa1e-4e2e-a36a-fb588e2948ca");
    private static final OffsetDateTime APPROVED_AT = OffsetDateTime.parse("2026-09-10T10:00:00+09:00");

    @Mock
    private TossPaymentClient tossPaymentClient;

    @Mock
    private SubmitSpendEventUseCase submitSpendEventUseCase;

    private TossPaymentWebhookService service;

    @BeforeEach
    void setUp() {
        service = new TossPaymentWebhookService(
                tossPaymentClient,
                new TossPaymentProperties(
                        true,
                        URI.create("https://api.tosspayments.com"),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5),
                        "test_ck",
                        "test_sk",
                        "test-mid",
                        USER_ID.toString()
                ),
                submitSpendEventUseCase
        );
    }

    @Test
    void verifiesAndSubmitsPaymentAndCompletedCancellations() {
        given(tossPaymentClient.getPayment("payment-key-1")).willReturn(payment());

        TossPaymentWebhookService.Result result = service.process(
                "PAYMENT_STATUS_CHANGED",
                "payment-key-1"
        );

        assertThat(result).isEqualTo(new TossPaymentWebhookService.Result(false, 2, 0));
        ArgumentCaptor<SubmitSpendEventCommand> command = ArgumentCaptor.forClass(
                SubmitSpendEventCommand.class
        );
        verify(submitSpendEventUseCase, org.mockito.Mockito.times(2)).submit(command.capture());
        assertThat(command.getAllValues()).containsExactly(
                new SubmitSpendEventCommand(
                        USER_ID,
                        SpendEventSource.TOSS_WEBHOOK,
                        "payment-key-1",
                        "테스트 주문 12800원 결제",
                        APPROVED_AT.toInstant()
                ),
                new SubmitSpendEventCommand(
                        USER_ID,
                        SpendEventSource.TOSS_WEBHOOK,
                        "cancel:cancel-transaction-1",
                        "테스트 주문 3000원 결제 취소",
                        OffsetDateTime.parse("2026-09-10T10:05:00+09:00").toInstant()
                )
        );
    }

    @Test
    void treatsWebhookRedeliveryAsSuccessfulDuplicate() {
        given(tossPaymentClient.getPayment("payment-key-1")).willReturn(payment());
        given(submitSpendEventUseCase.submit(any())).willThrow(new DuplicateSpendEventException());

        TossPaymentWebhookService.Result result = service.process(
                "PAYMENT_STATUS_CHANGED",
                "payment-key-1"
        );

        assertThat(result).isEqualTo(new TossPaymentWebhookService.Result(false, 0, 2));
    }

    @Test
    void rejectsPaymentFromAnotherMerchant() {
        Payment payment = new Payment(
                "other-mid",
                "payment-key-1",
                "order-1",
                "테스트 주문",
                12_800,
                "DONE",
                APPROVED_AT,
                List.of()
        );
        given(tossPaymentClient.getPayment("payment-key-1")).willReturn(payment);

        assertThatThrownBy(() -> service.process("PAYMENT_STATUS_CHANGED", "payment-key-1"))
                .isInstanceOf(TossPaymentVerificationException.class)
                .hasMessageContaining("상점");
        verify(submitSpendEventUseCase, never()).submit(any());
    }

    @Test
    void acknowledgesUnsupportedWebhookWithoutCallingToss() {
        TossPaymentWebhookService.Result result = service.process(
                "DEPOSIT_CALLBACK",
                "payment-key-1"
        );

        assertThat(result.ignored()).isTrue();
        verifyNoInteractions(tossPaymentClient, submitSpendEventUseCase);
    }

    private Payment payment() {
        return new Payment(
                "test-mid",
                "payment-key-1",
                "order-1",
                "  테스트   주문  ",
                12_800,
                "PARTIAL_CANCELED",
                APPROVED_AT,
                List.of(
                        new Cancel(
                                3_000,
                                "DONE",
                                OffsetDateTime.parse("2026-09-10T10:05:00+09:00"),
                                "cancel-transaction-1"
                        ),
                        new Cancel(
                                1_000,
                                "FAILED",
                                OffsetDateTime.parse("2026-09-10T10:06:00+09:00"),
                                "cancel-transaction-2"
                        )
                )
        );
    }
}
