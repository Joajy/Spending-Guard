package com.joajy.spendingguard.spendevent.infrastructure.config;

import com.joajy.spendingguard.spendevent.domain.policy.DeduplicationKeyGenerator;
import com.joajy.spendingguard.spendevent.domain.policy.MessageSanitizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 프레임워크에 의존하지 않는 소비 이벤트 도메인 정책을 Spring 빈으로 등록한다.
 *
 * <p><strong>포함 기능:</strong> 중복 키 생성과 메시지 정제 정책을 애플리케이션에서
 * 주입받을 수 있도록 조립한다. 정책 자체에는 Spring 애노테이션을 두지 않는다.
 *
 * <p><strong>존재 이유:</strong> 객체 생성 책임만 인프라 계층에 두면 도메인 정책은
 * 프레임워크 없이 빠른 단위 테스트로 검증할 수 있다. 이후 채널별 정제 정책이나 다른
 * 중복 키 구현이 필요해져도 서비스 코드를 바꾸지 않고 조립만 교체할 수 있다.
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
