package com.joajy.spendingguard.spendevent.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.service.exception.SpendEventNotFoundException;
import com.joajy.spendingguard.spendevent.service.port.outbound.LoadSpendEventDetailPort;
import com.joajy.spendingguard.spendevent.service.result.SpendEventDetail;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpendEventQueryServiceTest {

    private final StubLoadSpendEventDetailPort loadPort = new StubLoadSpendEventDetailPort();
    private final SpendEventQueryService queryService = new SpendEventQueryService(loadPort);

    @Test
    void returnsDetailLoadedByEventId() {
        UUID eventId = UUID.fromString("9bbd364c-a952-42aa-91cb-f607603aa7d5");
        SpendEventDetail expected = new SpendEventDetail(
                eventId,
                SpendEventSource.SIMULATOR,
                SpendEventStatus.RECEIVED,
                null,
                Instant.parse("2026-08-18T01:00:00Z"),
                null
        );
        loadPort.result = Optional.of(expected);

        SpendEventDetail actual = queryService.get(eventId);

        assertThat(actual).isEqualTo(expected);
        assertThat(loadPort.requestedEventId).isEqualTo(eventId);
    }

    @Test
    void rejectsUnknownEventId() {
        UUID eventId = UUID.fromString("b894a4c7-0c4b-453b-8c82-92bbcd6bd8eb");

        assertThatThrownBy(() -> queryService.get(eventId))
                .isInstanceOf(SpendEventNotFoundException.class)
                .hasMessageContaining(eventId.toString());
    }

    private static final class StubLoadSpendEventDetailPort implements LoadSpendEventDetailPort {

        private Optional<SpendEventDetail> result = Optional.empty();
        private UUID requestedEventId;

        @Override
        public Optional<SpendEventDetail> findById(UUID eventId) {
            requestedEventId = eventId;
            return result;
        }
    }
}

