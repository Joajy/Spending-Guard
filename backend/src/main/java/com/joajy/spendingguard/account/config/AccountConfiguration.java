package com.joajy.spendingguard.account.config;

import com.joajy.spendingguard.account.domain.policy.EmailNormalizer;
import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 계정 도메인의 상태 없는 정책 객체를 애플리케이션 실행 환경에 등록한다. */
@Configuration
class AccountConfiguration {

    @Bean
    EmailNormalizer emailNormalizer() {
        return new EmailNormalizer();
    }

    @Bean
    PasswordEncoder verificationCodeEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    SecureRandom verificationCodeRandom() {
        return new SecureRandom();
    }
}
