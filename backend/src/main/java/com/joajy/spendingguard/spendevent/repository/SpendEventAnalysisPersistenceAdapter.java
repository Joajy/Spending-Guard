package com.joajy.spendingguard.spendevent.repository;

import java.util.UUID;

import com.joajy.spendingguard.analysis.application.exception.SpendEventNotFoundException;
import com.joajy.spendingguard.analysis.application.model.SpendEventAnalysisTarget;
import com.joajy.spendingguard.analysis.application.port.outbound.LoadSpendEventForAnalysisPort;
import com.joajy.spendingguard.analysis.application.port.outbound.UpdateSpendEventStatusPort;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import org.springframework.stereotype.Component;

/**
 * 원천 이벤트 저장소에서 분석 입력을 읽고 처리 상태를 변경하는 출력 어댑터다.
 *
 * <p>분석 모듈이 JPA 엔티티나 저장소를 직접 알지 않도록 원천 이벤트 모듈의 영속성
 * 경계에서 필요한 값만 애플리케이션 모델로 변환한다.
 */
@Component
class SpendEventAnalysisPersistenceAdapter implements
        LoadSpendEventForAnalysisPort,
        UpdateSpendEventStatusPort {

    private final RawSpendEventJpaRepository repository;

    SpendEventAnalysisPersistenceAdapter(RawSpendEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SpendEventAnalysisTarget load(UUID eventId) {
        RawSpendEventEntity event = repository.findById(eventId)
                .orElseThrow(() -> new SpendEventNotFoundException(eventId));
        return new SpendEventAnalysisTarget(
                event.getId(),
                event.getSanitizedMessage(),
                event.getOccurredAt()
        );
    }

    @Override
    public void update(UUID eventId, SpendEventStatus status) {
        if (repository.updateStatus(eventId, status) != 1) {
            throw new SpendEventNotFoundException(eventId);
        }
    }
}
