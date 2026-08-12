package com.joajy.spendingguard.spendevent;

import java.net.URI;

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

    private final SpendEventService spendEventService;

    SpendEventController(SpendEventService spendEventService) {
        this.spendEventService = spendEventService;
    }

    @PostMapping
    public ResponseEntity<SpendEventAcceptedResponse> submit(
            @Valid @RequestBody SubmitSpendEventRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        SpendEventAcceptedResponse response = spendEventService.submit(request);
        URI location = uriBuilder.path("/api/v1/spend-events/{eventId}")
                .build(response.eventId());
        return ResponseEntity.accepted().location(location).body(response);
    }
}
