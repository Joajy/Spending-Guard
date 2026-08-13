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
