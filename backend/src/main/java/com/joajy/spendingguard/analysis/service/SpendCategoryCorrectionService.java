package com.joajy.spendingguard.analysis.service;

import java.time.Clock;

import com.joajy.spendingguard.analysis.domain.policy.SpendRiskClassifier;
import com.joajy.spendingguard.analysis.service.command.CorrectSpendCategoryCommand;
import com.joajy.spendingguard.analysis.service.exception.SpendCategoryCorrectionNotFoundException;
import com.joajy.spendingguard.analysis.service.exception.SpendCategoryNotEditableException;
import com.joajy.spendingguard.analysis.service.exception.SpendCategoryVersionConflictException;
import com.joajy.spendingguard.analysis.service.port.inbound.CorrectSpendCategoryUseCase;
import com.joajy.spendingguard.analysis.service.port.outbound.LoadSpendCategoryCorrectionTargetPort;
import com.joajy.spendingguard.analysis.service.port.outbound.StoreSpendCategoryOverridePort;
import com.joajy.spendingguard.analysis.service.result.SpendCategoryCorrectionResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 자동 분류 원본은 보존한 채 사용자의 카테고리 수정값과 파생 위험 신호를 저장한다.
 *
 * <p>클라이언트가 조회한 버전과 저장 시점의 버전이 일치할 때만 변경한다. 같은 내역을
 * 동시에 수정한 요청은 하나만 성공하며, 뒤늦은 요청은 최신 내역 재조회가 필요한 충돌로 반환한다.
 */
@Service
public class SpendCategoryCorrectionService implements CorrectSpendCategoryUseCase {

    private final LoadSpendCategoryCorrectionTargetPort loadTargetPort;
    private final StoreSpendCategoryOverridePort storeOverridePort;
    private final SpendRiskClassifier riskClassifier;
    private final Clock clock;

    public SpendCategoryCorrectionService(
            LoadSpendCategoryCorrectionTargetPort loadTargetPort,
            StoreSpendCategoryOverridePort storeOverridePort,
            SpendRiskClassifier riskClassifier,
            Clock clock
    ) {
        this.loadTargetPort = loadTargetPort;
        this.storeOverridePort = storeOverridePort;
        this.riskClassifier = riskClassifier;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SpendCategoryCorrectionResult correct(CorrectSpendCategoryCommand command) {
        if (command.expectedVersion() < 0) {
            throw new IllegalArgumentException("expectedVersion은 0 이상이어야 합니다.");
        }
        var target = loadTargetPort.find(command.userId(), command.eventId())
                .orElseThrow(() -> new SpendCategoryCorrectionNotFoundException(command.eventId()));
        if (!target.editable()) {
            throw new SpendCategoryNotEditableException();
        }
        if (target.version() != command.expectedVersion()) {
            throw new SpendCategoryVersionConflictException();
        }

        var assessment = riskClassifier.classify(
                command.category(), target.amount(), target.transactionType(), target.occurredAt()
        );
        var correctedAt = clock.instant();
        long version = storeOverridePort.store(
                command.userId(), command.eventId(), assessment,
                command.expectedVersion(), correctedAt
        ).orElseThrow(SpendCategoryVersionConflictException::new);

        return new SpendCategoryCorrectionResult(
                command.eventId(),
                target.originalCategory(),
                assessment.category(),
                assessment.fixedCost(),
                assessment.riskLevel(),
                assessment.reasonCode(),
                version,
                correctedAt
        );
    }
}
