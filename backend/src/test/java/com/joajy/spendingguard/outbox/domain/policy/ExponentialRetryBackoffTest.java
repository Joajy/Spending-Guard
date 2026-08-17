package com.joajy.spendingguard.outbox.domain.policy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ExponentialRetryBackoffTest {

    @Test
    void doublesDelayForEachPreviousAttempt() {
        ExponentialRetryBackoff backoff = new ExponentialRetryBackoff(
                Duration.ofSeconds(5),
                Duration.ofMinutes(5)
        );

        assertThat(backoff.delayAfter(0)).isEqualTo(Duration.ofSeconds(5));
        assertThat(backoff.delayAfter(1)).isEqualTo(Duration.ofSeconds(10));
        assertThat(backoff.delayAfter(2)).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void capsDelayAtConfiguredMaximum() {
        ExponentialRetryBackoff backoff = new ExponentialRetryBackoff(
                Duration.ofSeconds(5),
                Duration.ofMinutes(1)
        );

        assertThat(backoff.delayAfter(20)).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void rejectsInvalidConfigurationAndAttempts() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new ExponentialRetryBackoff(Duration.ZERO, Duration.ofSeconds(10))
        );
        assertThatIllegalArgumentException().isThrownBy(() ->
                new ExponentialRetryBackoff(Duration.ofSeconds(10), Duration.ofSeconds(5))
        );

        ExponentialRetryBackoff backoff = new ExponentialRetryBackoff(
                Duration.ofSeconds(5),
                Duration.ofSeconds(10)
        );
        assertThatIllegalArgumentException().isThrownBy(() -> backoff.delayAfter(-1));
    }
}

