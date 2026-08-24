package com.joajy.spendingguard.spendevent.controller;

import java.net.URI;
import java.time.YearMonth;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.controller.dto.request.SubmitSpendEventRequest;
import com.joajy.spendingguard.spendevent.controller.dto.response.SpendEventAcceptedResponse;
import com.joajy.spendingguard.spendevent.controller.dto.response.SpendEventDetailResponse;
import com.joajy.spendingguard.spendevent.controller.dto.response.SpendEventHistoryResponse;
import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import com.joajy.spendingguard.spendevent.service.port.inbound.GetSpendEventUseCase;
import com.joajy.spendingguard.spendevent.service.port.inbound.ListSpendEventsUseCase;
import com.joajy.spendingguard.spendevent.service.port.inbound.SubmitSpendEventUseCase;
import com.joajy.spendingguard.spendevent.service.result.SpendEventReceipt;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 소비 알림 접수와 사용자의 소비 내역 조회를 제공하는 HTTP 입력 어댑터다.
 *
 * <p>요청 검증과 HTTP 표현 변환만 담당하며, 실제 접수 규칙은
 * {@link SubmitSpendEventUseCase}에 위임한다. 저장소나 메시지 브로커를 직접 호출하지 않는다.
 *
 * <p>접수 성공 응답의 {@code 202 Accepted}는 원천 이벤트와 Outbox 저장이 완료되었고 후속
 * 분석은 비동기로 진행된다는 의미다. {@code Location} 헤더에는 접수된 이벤트의 식별
 * URI를 제공한다.
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/spend-events")
public class SpendEventController {

    private final SubmitSpendEventUseCase submitSpendEventUseCase;
    private final GetSpendEventUseCase getSpendEventUseCase;
    private final ListSpendEventsUseCase listSpendEventsUseCase;

    public SpendEventController(
            SubmitSpendEventUseCase submitSpendEventUseCase,
            GetSpendEventUseCase getSpendEventUseCase,
            ListSpendEventsUseCase listSpendEventsUseCase
    ) {
        this.submitSpendEventUseCase = submitSpendEventUseCase;
        this.getSpendEventUseCase = getSpendEventUseCase;
        this.listSpendEventsUseCase = listSpendEventsUseCase;
    }

    @GetMapping
    @PreAuthorize("@userScope.matches(authentication, #userId)")
    public SpendEventHistoryResponse list(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) SpendEventStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return SpendEventHistoryResponse.from(
                listSpendEventsUseCase.list(userId, month, status, category, cursor, size)
        );
    }

    /**
     * 소비 알림 하나를 분석 파이프라인에 접수한다.
     *
     * @param request 형식과 길이 검증을 통과한 HTTP 요청
     * @param uriBuilder 현재 요청 기준의 리소스 URI 생성기
     * @return 이벤트 ID와 접수 상태를 담은 {@code 202 Accepted} 응답
     */
    @PostMapping
    @PreAuthorize("@userScope.matches(authentication, #userId)")
    public ResponseEntity<SpendEventAcceptedResponse> submit(
            @PathVariable UUID userId,
            @Valid @RequestBody SubmitSpendEventRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        SpendEventReceipt receipt = submitSpendEventUseCase.submit(request.toCommand(userId));
        SpendEventAcceptedResponse response = SpendEventAcceptedResponse.from(receipt);
        URI location = uriBuilder.path("/api/v1/users/{userId}/spend-events/{eventId}")
                .build(userId, response.eventId());
        return ResponseEntity.accepted().location(location).body(response);
    }

    /**
     * 접수된 이벤트의 현재 처리 상태와 확보된 빠른 파싱 결과를 조회한다.
     *
     * @param eventId 접수 응답 또는 {@code Location} 헤더로 전달된 이벤트 식별자
     * @return 조회 시점의 상태와, 분석이 시작된 경우 빠른 파싱 결과
     */
    @GetMapping("/{eventId}")
    @PreAuthorize("@userScope.matches(authentication, #userId)")
    public SpendEventDetailResponse get(@PathVariable UUID userId, @PathVariable UUID eventId) {
        return SpendEventDetailResponse.from(getSpendEventUseCase.get(userId, eventId));
    }
}
