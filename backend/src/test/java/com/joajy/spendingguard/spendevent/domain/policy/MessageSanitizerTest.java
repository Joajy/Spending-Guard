package com.joajy.spendingguard.spendevent.domain.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSanitizerTest {

    private final MessageSanitizer sanitizer = new MessageSanitizer();

    @Test
    void masksLongNumbersAndEmailAddresses() {
        String message = "  test@example.com   테스트카드 1234-5678-9012-3456 12,800원 결제  ";

        String sanitized = sanitizer.sanitize(message);

        assertThat(sanitized)
                .isEqualTo("[EMAIL] 테스트카드 [REDACTED] 12,800원 결제")
                .doesNotContain("test@example.com", "1234-5678-9012-3456");
    }
}
