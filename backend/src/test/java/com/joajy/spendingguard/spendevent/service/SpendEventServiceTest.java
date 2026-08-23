package com.joajy.spendingguard.spendevent.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.joajy.spendingguard.spendevent.service.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.service.port.outbound.AppendSpendEventOutboxPort;
import com.joajy.spendingguard.spendevent.service.port.outbound.CheckSpendEventOwnerPort;
import com.joajy.spendingguard.spendevent.service.port.outbound.StoreRawSpendEventPort;
import com.joajy.spendingguard.spendevent.service.result.SpendEventReceipt;
import com.joajy.spendingguard.spendevent.domain.event.SpendEventReceived;
import com.joajy.spendingguard.spendevent.domain.model.RawSpendEvent;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import com.joajy.spendingguard.spendevent.domain.policy.DeduplicationKeyGenerator;
import com.joajy.spendingguard.spendevent.domain.policy.MessageSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpendEventServiceTest {
    private static final java.util.UUID USER_ID = java.util.UUID.fromString("3f6d4218-f5a6-48cf-9813-006779108c0d");

    private static final Instant RECEIVED_AT = Instant.parse("2026-08-14T00:00:00Z");

    @Mock
    private StoreRawSpendEventPort storeRawSpendEventPort;

    @Mock
    private AppendSpendEventOutboxPort appendSpendEventOutboxPort;
    @Mock private CheckSpendEventOwnerPort checkSpendEventOwnerPort;

    @Captor
    private ArgumentCaptor<RawSpendEvent> spendEventCaptor;

    @Captor
    private ArgumentCaptor<SpendEventReceived> outboxEventCaptor;

    private SpendEventService spendEventService;

    @BeforeEach
    void setUp() {
        spendEventService = new SpendEventService(
                storeRawSpendEventPort,
                checkSpendEventOwnerPort,
                appendSpendEventOutboxPort,
                new MessageSanitizer(),
                new DeduplicationKeyGenerator(),
                Clock.fixed(RECEIVED_AT, ZoneOffset.UTC)
        );
    }

    @Test
    void submitsSanitizedEventThroughOutboundPorts() {
        org.mockito.BDDMockito.given(checkSpendEventOwnerPort.exists(USER_ID)).willReturn(true);
        SpendEventReceipt receipt = spendEventService.submit(new SubmitSpendEventCommand(
                USER_ID,
                SpendEventSource.MANUAL_TEXT,
                "  manual-100  ",
                "test@example.com 테스트카드 1234-5678-9012-3456 12,800원 결제",
                Instant.parse("2026-08-13T23:50:00Z")
        ));

        verify(storeRawSpendEventPort).store(spendEventCaptor.capture());
        verify(appendSpendEventOutboxPort).append(outboxEventCaptor.capture());
        RawSpendEvent storedEvent = spendEventCaptor.getValue();
        SpendEventReceived outboxEvent = outboxEventCaptor.getValue();

        assertThat(storedEvent.id()).isEqualTo(receipt.eventId());
        assertThat(storedEvent.userId()).isEqualTo(USER_ID);
        assertThat(storedEvent.externalEventId()).isEqualTo("manual-100");
        assertThat(storedEvent.sanitizedMessage())
                .isEqualTo("[EMAIL] 테스트카드 [REDACTED] 12,800원 결제");
        assertThat(outboxEvent.eventId()).isEqualTo(receipt.eventId());
        assertThat(outboxEvent.userId()).isEqualTo(USER_ID);
        assertThat(outboxEvent.receivedAt()).isEqualTo(RECEIVED_AT);
        assertThat(receipt.status()).isEqualTo(SpendEventStatus.RECEIVED);
    }
}
