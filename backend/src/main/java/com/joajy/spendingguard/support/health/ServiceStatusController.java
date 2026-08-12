package com.joajy.spendingguard.support.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
public class ServiceStatusController {

    @GetMapping
    public ResponseEntity<ServiceStatusResponse> status() {
        return ResponseEntity.ok(new ServiceStatusResponse("spending-guard-api", "UP"));
    }

    public record ServiceStatusResponse(String service, String status) {
    }
}
