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
 *
 * <p>요청 검증과 HTTP 표현 변환만 담당하며, 실제 접수 규칙은
 * {@link SubmitSpendEventUseCase}에 위임한다. 저장소나 메시지 브로커를 직접 호출하지 않는다.
 *
 * <p>성공 응답의 {@code 202 Accepted}는 원천 이벤트와 Outbox 저장이 완료되었고 후속
 * 분석은 비동기로 진행된다는 의미다. {@code Location} 헤더에는 접수된 이벤트의 식별
 * URI를 제공한다.
 */
@RestController
@RequestMapping("/api/v1/spend-events")
public class SpendEventController {

    private final SubmitSpendEventUseCase submitSpendEventUseCase;

    public SpendEventController(SubmitSpendEventUseCase submitSpendEventUseCase) {
        this.submitSpendEventUseCase = submitSpendEventUseCase;
    }

    /**
     * 소비 알림 하나를 분석 파이프라인에 접수한다.
     *
     * @param request 형식과 길이 검증을 통과한 HTTP 요청
     * @param uriBuilder 현재 요청 기준의 리소스 URI 생성기
     * @return 이벤트 ID와 접수 상태를 담은 {@code 202 Accepted} 응답
     */
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
