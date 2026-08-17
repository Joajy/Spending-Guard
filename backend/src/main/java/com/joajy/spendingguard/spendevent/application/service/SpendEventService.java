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
 * 외부 식별자를 정규화하고 중복 키와 정제 메시지를 만든 뒤, 원천 이벤트와 Outbox 이벤트를 하나의 트랜잭션으로 저장한다.
 * 이 원자적 저장으로 데이터는 남았지만 분석 이벤트가 유실되는 상태를 방지한다.
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
