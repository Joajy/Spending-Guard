package com.joajy.spendingguard.support.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 애플리케이션 프로세스가 HTTP 요청을 받을 수 있는지 확인하는 간단한 상태 API다.
 *
 * <p><strong>도입 배경:</strong> 업무 API를 상태 점검에 사용하면 테스트 데이터가
 * 생성되거나 입력 검증 실패가 서비스 장애로 오인될 수 있다. 별도 엔드포인트를 두어
 * 로컬 실행, 컨테이너 점검, 배포 확인이 부작용 없이 프로세스 응답 여부를 확인하게 한다.
 *
 * <p><strong>보장 범위:</strong> 이 응답은 웹 애플리케이션이 요청을 처리할 수 있다는
 * liveness 신호다. PostgreSQL과 Kafka의 준비 상태까지 보장하는 readiness 검사는 아니며,
 * 인프라 배포 단계에서 Spring Boot Actuator 기반 점검으로 확장할 예정이다.
 */
@RestController
@RequestMapping("/api/v1/status")
public class ServiceStatusController {

    /**
     * 프로세스가 HTTP 요청을 처리할 수 있는지 확인한다.
     *
     * @return 서비스 이름과 {@code UP} 상태
     */
    @GetMapping
    public ResponseEntity<ServiceStatusResponse> status() {
        return ResponseEntity.ok(new ServiceStatusResponse("spending-guard-api", "UP"));
    }

    /**
     * 상태 확인 API가 반환하는 서비스 식별자와 현재 가동 상태를 표현한다.
     *
     * @param service 상태를 응답한 애플리케이션 이름
     * @param status 프로세스의 현재 가동 상태
     */
    public record ServiceStatusResponse(String service, String status) {
    }
}
