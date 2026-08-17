package com.joajy.spendingguard.support.time;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 애플리케이션 전반에서 사용할 기준 시계를 제공한다.
 * UTC 시계를 의존성으로 주입해 서버 시간대 차이를 제거하고, 테스트에서는 고정 시계로 교체할 수 있게 한다.
 */
@Configuration
public class TimeConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
