package com.joajy.spendingguard.spendevent.infrastructure.config;

import com.joajy.spendingguard.spendevent.domain.policy.DeduplicationKeyGenerator;
import com.joajy.spendingguard.spendevent.domain.policy.MessageSanitizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 프레임워크에 의존하지 않는 소비 이벤트 도메인 정책을 Spring 빈으로 등록한다.
 * 객체 생성 책임만 인프라 계층에 두어 정책 클래스는 단순 단위 테스트로 검증할 수 있게 한다.
 */
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
