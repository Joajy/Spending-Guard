package com.joajy.spendingguard.integration.toss.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Toss Payments 입력 Adapter의 외부 설정을 등록한다. */
@Configuration
@EnableConfigurationProperties(TossPaymentProperties.class)
class TossPaymentConfiguration {
}
