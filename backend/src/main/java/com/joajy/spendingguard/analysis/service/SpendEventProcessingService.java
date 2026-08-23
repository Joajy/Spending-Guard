package com.joajy.spendingguard.analysis.service;

import java.time.Clock;
import java.time.Instant;

import com.joajy.spendingguard.analysis.service.command.ProcessSpendEventCommand;
import com.joajy.spendingguard.analysis.service.model.SpendEventAnalysisTarget;
import com.joajy.spendingguard.analysis.service.port.inbound.ProcessSpendEventUseCase;
import com.joajy.spendingguard.analysis.service.port.outbound.LoadSpendEventForAnalysisPort;
import com.joajy.spendingguard.analysis.service.port.outbound.StoreFastParseResultPort;
import com.joajy.spendingguard.analysis.service.port.outbound.TryClaimProcessedEventPort;
import com.joajy.spendingguard.analysis.service.port.outbound.UpdateSpendEventStatusPort;
import com.joajy.spendingguard.analysis.service.result.SpendEventProcessingResult;
import com.joajy.spendingguard.analysis.domain.model.FastParseOutcome;
import com.joajy.spendingguard.analysis.domain.policy.FastSpendEventParser;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka로 전달된 소비 이벤트를 멱등하게 선점하고 결정론적 규칙으로 빠른 분석한다.
 *
 * <p><strong>트랜잭션 경계:</strong> 처리 선점, 원천 조회, 파싱 결과 저장, 상태 변경을
 * 하나의 트랜잭션으로 묶는다. 중간 실패 시 선점 기록도 롤백되므로 Kafka 재전달이
 * 이벤트를 다시 처리할 수 있다.
 *
 * <p><strong>중복 전달:</strong> {@code eventId + consumerName} 고유 제약을 가진 선점
 * 테이블에 {@code INSERT ... ON CONFLICT DO NOTHING}을 실행한다. 이미 처리한 메시지는
 * 성공으로 종료해 Kafka 오프셋만 진행시키고 업무 데이터는 다시 변경하지 않는다.
 */
@Service
public class SpendEventProcessingService implements ProcessSpendEventUseCase {

    static final String CONSUMER_NAME = "spend-event-fast-parser-v1";
    static final String PARSER_VERSION = "fast-parser-v1";

    private final TryClaimProcessedEventPort tryClaimProcessedEventPort;
    private final LoadSpendEventForAnalysisPort loadSpendEventForAnalysisPort;
    private final StoreFastParseResultPort storeFastParseResultPort;
    private final UpdateSpendEventStatusPort updateSpendEventStatusPort;
    private final FastSpendEventParser parser;
    private final Clock clock;

    public SpendEventProcessingService(
            TryClaimProcessedEventPort tryClaimProcessedEventPort,
            LoadSpendEventForAnalysisPort loadSpendEventForAnalysisPort,
            StoreFastParseResultPort storeFastParseResultPort,
            UpdateSpendEventStatusPort updateSpendEventStatusPort,
            FastSpendEventParser parser,
            Clock clock
    ) {
        this.tryClaimProcessedEventPort = tryClaimProcessedEventPort;
        this.loadSpendEventForAnalysisPort = loadSpendEventForAnalysisPort;
        this.storeFastParseResultPort = storeFastParseResultPort;
        this.updateSpendEventStatusPort = updateSpendEventStatusPort;
        this.parser = parser;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SpendEventProcessingResult process(ProcessSpendEventCommand command) {
        Instant processedAt = clock.instant();
        if (!tryClaimProcessedEventPort.tryClaim(
                command.eventId(),
                CONSUMER_NAME,
                processedAt
        )) {
            return SpendEventProcessingResult.ALREADY_PROCESSED;
        }

        SpendEventAnalysisTarget target = loadSpendEventForAnalysisPort.load(command.eventId());
        FastParseOutcome outcome = parser.parse(target.sanitizedMessage());
        storeFastParseResultPort.store(
                command.eventId(),
                outcome,
                PARSER_VERSION,
                processedAt
        );

        if (outcome.needsReview()) {
            updateSpendEventStatusPort.update(command.eventId(), SpendEventStatus.NEEDS_REVIEW);
            return SpendEventProcessingResult.NEEDS_REVIEW;
        }

        updateSpendEventStatusPort.update(command.eventId(), SpendEventStatus.ANALYZING);
        return SpendEventProcessingResult.PROCESSED;
    }
}
