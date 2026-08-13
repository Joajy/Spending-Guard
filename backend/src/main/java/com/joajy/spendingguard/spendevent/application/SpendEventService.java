package com.joajy.spendingguard.spendevent.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joajy.spendingguard.outbox.OutboxEvent;
import com.joajy.spendingguard.outbox.OutboxEventRepository;
import com.joajy.spendingguard.spendevent.domain.DeduplicationKeyGenerator;
import com.joajy.spendingguard.spendevent.domain.MessageSanitizer;
import com.joajy.spendingguard.spendevent.domain.SpendEventStatus;
import com.joajy.spendingguard.spendevent.persistence.RawSpendEvent;
import com.joajy.spendingguard.spendevent.persistence.RawSpendEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SpendEventService {

    private final RawSpendEventRepository rawSpendEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final MessageSanitizer messageSanitizer;
    private final DeduplicationKeyGenerator deduplicationKeyGenerator;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SpendEventService(
            RawSpendEventRepository rawSpendEventRepository,
            OutboxEventRepository outboxEventRepository,
            MessageSanitizer messageSanitizer,
            DeduplicationKeyGenerator deduplicationKeyGenerator,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.rawSpendEventRepository = rawSpendEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.messageSanitizer = messageSanitizer;
        this.deduplicationKeyGenerator = deduplicationKeyGenerator;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

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

        try {
            rawSpendEventRepository.saveAndFlush(spendEvent);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateSpendEventException();
        }

        outboxEventRepository.save(new OutboxEvent(
                UUID.randomUUID(),
                eventId,
                createOutboxPayload(spendEvent),
                receivedAt
        ));

        return new SpendEventReceipt(eventId, SpendEventStatus.RECEIVED, receivedAt);
    }

    private String normalizeExternalEventId(String externalEventId) {
        return StringUtils.hasText(externalEventId) ? externalEventId.trim() : null;
    }

    private String createOutboxPayload(RawSpendEvent event) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "schemaVersion", 1,
                    "eventId", event.getId(),
                    "source", event.getSource(),
                    "receivedAt", event.getReceivedAt()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("소비 이벤트 발행 데이터를 생성할 수 없습니다.", exception);
        }
    }
}
