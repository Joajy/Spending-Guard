package com.joajy.spendingguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spending Guard 백엔드의 Spring Boot 실행 진입점이다.
 * 최상위 패키지에서 컴포넌트 탐색과 자동 구성을 시작해 소비 이벤트 접수와 Outbox 발행 모듈을 하나의 애플리케이션으로 조립한다.
 */
@SpringBootApplication
public class SpendingGuardApplication {

    /**
     * Spring 애플리케이션 컨텍스트를 생성하고 HTTP 서버를 시작한다.
     *
     * @param args Spring Boot에 전달할 명령행 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(SpendingGuardApplication.class, args);
    }
}
