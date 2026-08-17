package com.joajy.spendingguard.spendevent.api.controller;

import java.net.URI;

import com.joajy.spendingguard.spendevent.api.dto.request.SubmitSpendEventRequest;
import com.joajy.spendingguard.spendevent.api.dto.response.SpendEventAcceptedResponse;
import com.joajy.spendingguard.spendevent.application.port.inbound.SubmitSpendEventUseCase;
import com.joajy.spendingguard.spendevent.application.result.SpendEventReceipt;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 소비 알림 텍스트를 접수하는 HTTP 입력 어댑터다.
 * 요청 DTO를 입력 포트로 전달하고 비동기 처리 접수 결과를 {@code 202 Accepted}와 리소스 위치로 변환한다.
 * 저장소나 메시지 브로커를 직접 호출하지 않아 전송 계층과 유스케이스의 경계를 유지한다.
 */
@RestController
@RequestMapping("/api/v1/spend-events")
public class SpendEventController {

    private final SubmitSpendEventUseCase submitSpendEventUseCase;

    public SpendEventController(SubmitSpendEventUseCase submitSpendEventUseCase) {
        this.submitSpendEventUseCase = submitSpendEventUseCase;
    }

    @PostMapping
    public ResponseEntity<SpendEventAcceptedResponse> submit(
            @Valid @RequestBody SubmitSpendEventRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        SpendEventReceipt receipt = submitSpendEventUseCase.submit(request.toCommand());
        SpendEventAcceptedResponse response = SpendEventAcceptedResponse.from(receipt);
        URI location = uriBuilder.path("/api/v1/spend-events/{eventId}")
                .build(response.eventId());
        return ResponseEntity.accepted().location(location).body(response);
    }
}
