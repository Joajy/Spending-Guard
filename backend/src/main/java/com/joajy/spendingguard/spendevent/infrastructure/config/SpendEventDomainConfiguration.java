package com.joajy.spendingguard.spendevent.infrastructure.config;

import com.joajy.spendingguard.spendevent.domain.policy.DeduplicationKeyGenerator;
import com.joajy.spendingguard.spendevent.domain.policy.MessageSanitizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SpendEventDomainConfiguration {

    @Bean
    DeduplicationKeyGenerator deduplicationKeyGenerator() {
        return new DeduplicationKeyGenerator();
    }

    @Bean
    MessageSanitizer messageSanitizer() {
        return new MessageSanitizer();
    }
}
