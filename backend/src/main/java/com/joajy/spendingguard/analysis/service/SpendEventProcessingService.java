package com.joajy.spendingguard.analysis.service;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

import com.joajy.spendingguard.analysis.domain.model.FastParseOutcome;
import com.joajy.spendingguard.analysis.domain.model.SpendRiskAssessment;
import com.joajy.spendingguard.analysis.domain.model.TransactionType;
import com.joajy.spendingguard.analysis.domain.policy.FastSpendEventParser;
import com.joajy.spendingguard.analysis.domain.policy.SpendRiskClassifier;
import com.joajy.spendingguard.analysis.service.command.ProcessSpendEventCommand;
import com.joajy.spendingguard.analysis.service.model.SpendEventAnalysisTarget;
import com.joajy.spendingguard.analysis.service.port.inbound.ProcessSpendEventUseCase;
import com.joajy.spendingguard.analysis.service.port.outbound.ApplyBudgetConsumptionPort;
import com.joajy.spendingguard.analysis.service.port.outbound.LoadSpendEventForAnalysisPort;
import com.joajy.spendingguard.analysis.service.port.outbound.StoreFastParseResultPort;
import com.joajy.spendingguard.analysis.service.port.outbound.TryClaimProcessedEventPort;
import com.joajy.spendingguard.analysis.service.port.outbound.UpdateSpendEventStatusPort;
import com.joajy.spendingguard.analysis.service.result.SpendEventProcessingResult;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kafka 소비 이벤트를 멱등하게 선점하고 분석 결과와 예산 증감을 함께 반영한다. */
@Service
public class SpendEventProcessingService implements ProcessSpendEventUseCase {

    static final String CONSUMER_NAME = "spend-event-fast-parser-v1";
    static final String PARSER_VERSION = "fast-parser-v1";

    private final TryClaimProcessedEventPort tryClaimProcessedEventPort;
    private final LoadSpendEventForAnalysisPort loadSpendEventForAnalysisPort;
    private final StoreFastParseResultPort storeFastParseResultPort;
    private final UpdateSpendEventStatusPort updateSpendEventStatusPort;
    private final ApplyBudgetConsumptionPort applyBudgetConsumptionPort;
    private final FastSpendEventParser parser;
    private final SpendRiskClassifier riskClassifier;
    private final Clock clock;

    public SpendEventProcessingService(
            TryClaimProcessedEventPort tryClaimProcessedEventPort,
            LoadSpendEventForAnalysisPort loadSpendEventForAnalysisPort,
            StoreFastParseResultPort storeFastParseResultPort,
            UpdateSpendEventStatusPort updateSpendEventStatusPort,
            ApplyBudgetConsumptionPort applyBudgetConsumptionPort,
            FastSpendEventParser parser,
            SpendRiskClassifier riskClassifier,
            Clock clock
    ) {
        this.tryClaimProcessedEventPort = tryClaimProcessedEventPort;
        this.loadSpendEventForAnalysisPort = loadSpendEventForAnalysisPort;
        this.storeFastParseResultPort = storeFastParseResultPort;
        this.updateSpendEventStatusPort = updateSpendEventStatusPort;
        this.applyBudgetConsumptionPort = applyBudgetConsumptionPort;
        this.parser = parser;
        this.riskClassifier = riskClassifier;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SpendEventProcessingResult process(ProcessSpendEventCommand command) {
        Instant processedAt = clock.instant();
        if (!tryClaimProcessedEventPort.tryClaim(command.eventId(), CONSUMER_NAME, processedAt)) {
            return SpendEventProcessingResult.ALREADY_PROCESSED;
        }

        SpendEventAnalysisTarget target = loadSpendEventForAnalysisPort.load(command.eventId());
        FastParseOutcome outcome = parser.parse(target.sanitizedMessage());
        SpendRiskAssessment riskAssessment = outcome.needsReview() ? null : riskClassifier.classify(
                target.sanitizedMessage(),
                outcome.amount(),
                outcome.transactionType(),
                target.occurredAt()
        );
        storeFastParseResultPort.store(
                command.eventId(),
                outcome,
                riskAssessment,
                PARSER_VERSION,
                processedAt
        );

        if (outcome.needsReview()) {
            updateSpendEventStatusPort.update(command.eventId(), SpendEventStatus.NEEDS_REVIEW);
            return SpendEventProcessingResult.NEEDS_REVIEW;
        }

        if (target.userId() != null) {
            Instant occurredAt = target.occurredAt() == null ? processedAt : target.occurredAt();
            applyBudgetConsumptionPort.apply(
                    target.userId(),
                    command.eventId(),
                    YearMonth.from(occurredAt.atZone(ZoneOffset.UTC)),
                    signedAmount(outcome),
                    processedAt
            );
        }

        updateSpendEventStatusPort.update(command.eventId(), SpendEventStatus.ANALYZING);
        return SpendEventProcessingResult.PROCESSED;
    }

    private long signedAmount(FastParseOutcome outcome) {
        long amount = outcome.amount().longValueExact();
        return outcome.transactionType() == TransactionType.PAYMENT ? amount : -amount;
    }
}
