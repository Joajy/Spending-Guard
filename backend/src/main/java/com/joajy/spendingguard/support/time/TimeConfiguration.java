package com.joajy.spendingguard.support.time;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 애플리케이션 전반에서 사용할 기준 시계를 제공한다.
 *
 * <p><strong>도입 배경:</strong> 서비스 코드에서 {@code Instant.now()}를 직접 호출하면
 * 재시도 시각, 임대 만료, 접수 시각을 경계값에서 재현하기 어렵다. 서버의 지역 시간대가
 * 다를 때도 테스트와 운영 결과가 달라질 수 있다.
 *
 * <p><strong>존재 이유:</strong> UTC {@link Clock}을 하나의 의존성으로 제공해 모든 시간
 * 계산의 기준을 맞춘다. 테스트에서는 고정 시계로 교체해 임대 만료와 지수 백오프를
 * 대기 없이 결정적으로 검증할 수 있다.
 */
@Configuration
public class TimeConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
