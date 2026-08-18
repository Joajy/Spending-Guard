package com.joajy.spendingguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spending Guard 백엔드의 Spring Boot 실행 진입점이다.
 *
 * <p><strong>도입 배경:</strong> 초기 서비스는 한 사람이 운영할 수 있는 배포 단위를
 * 유지하면서도 소비 이벤트 접수와 비동기 발행의 경계는 분명해야 한다. 그래서 여러
 * 마이크로서비스를 먼저 만들지 않고 하나의 실행 파일 안에서 기능별 패키지를 조립한다.
 *
 * <p><strong>현재 포함 기능:</strong> HTTP 소비 알림 접수, 입력 정제와 중복 방지,
 * 원천 이벤트와 Outbox의 원자적 저장, Kafka 발행과 재시도, 상태 확인 API를 시작한다.
 * 최상위 패키지에 위치해 하위 기능 패키지만 컴포넌트 탐색 대상으로 삼는다.
 *
 * <p><strong>존재 이유:</strong> 프레임워크 부트스트랩을 업무 클래스와 분리하면 도메인
 * 코드는 실행 방식에 의존하지 않는다. 이후 API와 Worker의 실행 단위를 나누더라도
 * 이 클래스는 애플리케이션 조립 지점으로만 변경되고 업무 규칙은 유지된다.
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
