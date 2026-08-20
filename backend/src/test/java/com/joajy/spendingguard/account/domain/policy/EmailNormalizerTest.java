package com.joajy.spendingguard.account.domain.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailNormalizerTest {

    private final EmailNormalizer normalizer = new EmailNormalizer();

    @Test
    void removesOuterWhitespaceAndNormalizesCase() {
        assertThat(normalizer.normalize("  User.Name@Example.COM "))
                .isEqualTo("user.name@example.com");
    }
}
