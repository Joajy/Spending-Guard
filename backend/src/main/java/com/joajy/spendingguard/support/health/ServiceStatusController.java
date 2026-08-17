package com.joajy.spendingguard.support.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 애플리케이션 프로세스가 HTTP 요청을 받을 수 있는지 확인하는 간단한 상태 API다.
 * 로컬 실행과 외부 점검 도구가 업무 API를 호출하지 않고도 서비스 기동 여부를 확인할 수 있게 한다.
 */
@RestController
@RequestMapping("/api/v1/status")
public class ServiceStatusController {

    @GetMapping
    public ResponseEntity<ServiceStatusResponse> status() {
        return ResponseEntity.ok(new ServiceStatusResponse("spending-guard-api", "UP"));
    }

    /**
     * 상태 확인 API가 반환하는 서비스 식별자와 현재 가동 상태를 표현한다.
     */
    public record ServiceStatusResponse(String service, String status) {
    }
}
