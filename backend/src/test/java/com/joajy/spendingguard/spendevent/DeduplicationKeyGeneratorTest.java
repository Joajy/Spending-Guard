package com.joajy.spendingguard.spendevent;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeduplicationKeyGeneratorTest {

    private final DeduplicationKeyGenerator generator = new DeduplicationKeyGenerator();

    @Test
    void ignoresWhitespaceDifferencesInManualMessages() {
        Instant occurredAt = Instant.parse("2026-08-13T01:00:00Z");

        String first = generator.generate(SpendEventSource.MANUAL_TEXT, null, "테스트상점  12,800원", occurredAt);
        String second = generator.generate(SpendEventSource.MANUAL_TEXT, null, " 테스트상점 12,800원 ", occurredAt);

        assertThat(first).isEqualTo(second).hasSize(64);
    }

    @Test
    void usesExternalEventIdInsteadOfMessageWhenItExists() {
        String first = generator.generate(SpendEventSource.SIMULATOR, "event-100", "첫 번째 메시지", null);
        String second = generator.generate(SpendEventSource.SIMULATOR, "event-100", "수정된 메시지", null);

        assertThat(first).isEqualTo(second);
    }
}
