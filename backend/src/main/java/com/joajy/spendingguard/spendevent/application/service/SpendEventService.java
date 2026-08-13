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
