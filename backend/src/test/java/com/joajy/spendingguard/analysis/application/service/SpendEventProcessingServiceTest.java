package com.joajy.spendingguard.analysis.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.joajy.spendingguard.analysis.application.command.ProcessSpendEventCommand;
import com.joajy.spendingguard.analysis.application.model.SpendEventAnalysisTarget;
import com.joajy.spendingguard.analysis.application.port.outbound.LoadSpendEventForAnalysisPort;
import com.joajy.spendingguard.analysis.application.port.outbound.StoreFastParseResultPort;
import com.joajy.spendingguard.analysis.application.port.outbound.TryClaimProcessedEventPort;
import com.joajy.spendingguard.analysis.application.port.outbound.UpdateSpendEventStatusPort;
import com.joajy.spendingguard.analysis.application.result.SpendEventProcessingResult;
import com.joajy.spendingguard.analysis.domain.policy.FastSpendEventParser;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpendEventProcessingServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T01:00:00Z");

    @Mock
    private TryClaimProcessedEventPort tryClaimProcessedEventPort;

    @Mock
    private LoadSpendEventForAnalysisPort loadSpendEventForAnalysisPort;

    @Mock
    private StoreFastParseResultPort storeFastParseResultPort;

    @Mock
    private UpdateSpendEventStatusPort updateSpendEventStatusPort;

    private SpendEventProcessingService service;

    @BeforeEach
    void setUp() {
        service = new SpendEventProcessingService(
                tryClaimProcessedEventPort,
                loadSpendEventForAnalysisPort,
                storeFastParseResultPort,
                updateSpendEventStatusPort,
                new FastSpendEventParser(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void storesFastParseResultAndMovesEventToAnalyzing() {
        UUID eventId = UUID.randomUUID();
        given(tryClaimProcessedEventPort.tryClaim(
                eventId,
                SpendEventProcessingService.CONSUMER_NAME,
                NOW
        )).willReturn(true);
        given(loadSpendEventForAnalysisPort.load(eventId)).willReturn(new SpendEventAnalysisTarget(
                eventId,
                "쿠팡 12,800원 결제",
                NOW.minusSeconds(10)
        ));

        var result = service.process(new ProcessSpendEventCommand(eventId));

        assertThat(result).isEqualTo(SpendEventProcessingResult.PROCESSED);
        verify(storeFastParseResultPort).store(
                eq(eventId),
                any(),
                eq(SpendEventProcessingService.PARSER_VERSION),
                eq(NOW)
        );
        verify(updateSpendEventStatusPort).update(eventId, SpendEventStatus.ANALYZING);
    }

    @Test
    void marksEventForReviewWhenRequiredFieldsCannotBeParsed() {
        UUID eventId = UUID.randomUUID();
        given(tryClaimProcessedEventPort.tryClaim(any(), any(), any())).willReturn(true);
        given(loadSpendEventForAnalysisPort.load(eventId)).willReturn(new SpendEventAnalysisTarget(
                eventId,
                "결제 금액 확인 필요",
                NOW
        ));

        var result = service.process(new ProcessSpendEventCommand(eventId));

        assertThat(result).isEqualTo(SpendEventProcessingResult.NEEDS_REVIEW);
        verify(updateSpendEventStatusPort).update(eventId, SpendEventStatus.NEEDS_REVIEW);
    }

    @Test
    void skipsAllBusinessWritesWhenMessageWasAlreadyProcessed() {
        UUID eventId = UUID.randomUUID();
        given(tryClaimProcessedEventPort.tryClaim(any(), any(), any())).willReturn(false);

        var result = service.process(new ProcessSpendEventCommand(eventId));

        assertThat(result).isEqualTo(SpendEventProcessingResult.ALREADY_PROCESSED);
        verify(loadSpendEventForAnalysisPort, never()).load(any());
        verify(storeFastParseResultPort, never()).store(any(), any(), any(), any());
        verify(updateSpendEventStatusPort, never()).update(any(), any());
    }
}
