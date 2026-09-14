package com.joajy.spendingguard.integration.toss.controller;

import com.joajy.spendingguard.integration.toss.service.TossPaymentWebhookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Toss Payments가 전송한 결제 상태 변경을 받는 공개 HTTP 입력 Adapter다. */
@RestController
@RequestMapping("/api/v1/integrations/toss-payments/webhook")
public class TossPaymentWebhookController {

    private final TossPaymentWebhookService service;

    public TossPaymentWebhookController(TossPaymentWebhookService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Response> receive(@Valid @RequestBody Request request) {
        TossPaymentWebhookService.Result result = service.process(
                request.eventType(),
                request.data().paymentKey()
        );
        return ResponseEntity.ok(new Response(
                result.ignored(),
                result.acceptedCount(),
                result.duplicateCount()
        ));
    }

    public record Request(
            @NotBlank String eventType,
            @Valid @NotNull PaymentData data
    ) {
    }

    public record PaymentData(@NotBlank String paymentKey) {
    }

    public record Response(boolean ignored, int acceptedCount, int duplicateCount) {
    }
}
