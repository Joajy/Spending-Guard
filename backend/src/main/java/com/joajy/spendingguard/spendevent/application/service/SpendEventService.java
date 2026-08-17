package com.joajy.spendingguard.spendevent.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.application.command.SubmitSpendEventCommand;
import com.joajy.spendingguard.spendevent.application.port.inbound.SubmitSpendEventUseCase;
import com.joajy.spendingguard.spendevent.application.port.outbound.AppendSpendEventOutboxPort;
import com.joajy.spendingguard.spendevent.application.port.outbound.StoreRawSpendEventPort;
import com.joajy.spendingguard.spendevent.application.result.SpendEventReceipt;
import com.joajy.spendingguard.spendevent.domain.event.SpendEventReceived;
import com.joajy.spendingguard.spendevent.domain.model.RawSpendEvent;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import com.joajy.spendingguard.spendevent.domain.policy.DeduplicationKeyGenerator;
import com.joajy.spendingguard.spendevent.domain.policy.MessageSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 소비 알림을 안전한 원천 이벤트로 접수하고 후속 처리를 위한 도메인 이벤트를 기록하는 유스케이스다.
 *
 * <p><strong>처리 순서:</strong> 서버 식별자와 접수 시각을 만든 뒤 외부 식별자를
 * 정규화하고, 중복 키를 계산하고, 메시지의 민감 정보를 제거한다. 정제가 끝난
 * {@link RawSpendEvent}만 영속성 포트로 전달한다.
 *
 * <p><strong>트랜잭션 경계:</strong> 원천 이벤트와 {@link SpendEventReceived} Outbox 행을
 * 하나의 트랜잭션으로 저장한다. 어느 한쪽이라도 실패하면 전체 접수를 롤백해 저장된
 * 소비 데이터에 대응하는 분석 이벤트가 유실되지 않게 한다.
 *
 * <p><strong>동시 중복 요청:</strong> 사전 조회로 중복을 판단하지 않는다. 여러 요청이
 * 동시에 들어와도 데이터베이스 고유 제약이 최종 승자를 정하고, 나머지는 도메인 의미의
 * 중복 접수 예외로 반환된다.
 */
@Service
public class SpendEventService implements SubmitSpendEventUseCase {

    private final StoreRawSpendEventPort storeRawSpendEventPort;
    private final AppendSpendEventOutboxPort appendSpendEventOutboxPort;
    private final MessageSanitizer messageSanitizer;
    private final DeduplicationKeyGenerator deduplicationKeyGenerator;
    private final Clock clock;

    public SpendEventService(
            StoreRawSpendEventPort storeRawSpendEventPort,
            AppendSpendEventOutboxPort appendSpendEventOutboxPort,
            MessageSanitizer messageSanitizer,
            DeduplicationKeyGenerator deduplicationKeyGenerator,
            Clock clock
    ) {
        this.storeRawSpendEventPort = storeRawSpendEventPort;
        this.appendSpendEventOutboxPort = appendSpendEventOutboxPort;
        this.messageSanitizer = messageSanitizer;
        this.deduplicationKeyGenerator = deduplicationKeyGenerator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SpendEventReceipt submit(SubmitSpendEventCommand command) {
        UUID eventId = UUID.randomUUID();
        Instant receivedAt = clock.instant();
        String externalEventId = normalizeExternalEventId(command.externalEventId());
        String deduplicationKey = deduplicationKeyGenerator.generate(
                command.source(),
                externalEventId,
                command.message(),
                command.occurredAt()
        );

        RawSpendEvent spendEvent = new RawSpendEvent(
                eventId,
                command.source(),
                externalEventId,
                deduplicationKey,
                messageSanitizer.sanitize(command.message()),
                SpendEventStatus.RECEIVED,
                command.occurredAt(),
                receivedAt
        );

        storeRawSpendEventPort.store(spendEvent);
        appendSpendEventOutboxPort.append(new SpendEventReceived(
                eventId,
                command.source(),
                receivedAt
        ));

        return new SpendEventReceipt(eventId, SpendEventStatus.RECEIVED, receivedAt);
    }

    private String normalizeExternalEventId(String externalEventId) {
        return StringUtils.hasText(externalEventId) ? externalEventId.trim() : null;
    }
}
