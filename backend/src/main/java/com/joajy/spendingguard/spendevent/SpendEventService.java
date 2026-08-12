package com.joajy.spendingguard.spendevent;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    SpendEventService(
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
    public SpendEventAcceptedResponse submit(SubmitSpendEventRequest request) {
        UUID eventId = UUID.randomUUID();
        Instant receivedAt = clock.instant();
        String externalEventId = normalizeExternalEventId(request.externalEventId());
        String deduplicationKey = deduplicationKeyGenerator.generate(
                request.source(),
                externalEventId,
                request.message(),
                request.occurredAt()
        );

        RawSpendEvent spendEvent = new RawSpendEvent(
                eventId,
                request.source(),
                externalEventId,
                deduplicationKey,
                messageSanitizer.sanitize(request.message()),
                SpendEventStatus.RECEIVED,
                request.occurredAt(),
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

        return new SpendEventAcceptedResponse(eventId, SpendEventStatus.RECEIVED, receivedAt);
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
