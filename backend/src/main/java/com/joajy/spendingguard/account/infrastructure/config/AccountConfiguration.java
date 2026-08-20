package com.joajy.spendingguard.account.infrastructure.config;

import com.joajy.spendingguard.account.domain.policy.EmailNormalizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 계정 도메인의 상태 없는 정책 객체를 애플리케이션 실행 환경에 등록한다. */
@Configuration
class AccountConfiguration {

    @Bean
    EmailNormalizer emailNormalizer() {
        return new EmailNormalizer();
    }
}
