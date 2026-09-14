package com.joajy.spendingguard.integration.toss.controller;

import java.util.UUID;

import com.joajy.spendingguard.integration.toss.service.TossTestCheckoutService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 사용자가 Toss 결제창을 시연할 테스트 주문 API다. */
@RestController
@RequestMapping("/api/v1/users/{userId}/integrations/toss-payments/test-orders")
public class TossTestCheckoutController {

    private final TossTestCheckoutService service;

    public TossTestCheckoutController(TossTestCheckoutService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@userScope.matches(authentication, #userId)")
    public TossTestCheckoutService.CreatedOrder create(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateRequest request
    ) {
        return service.create(userId, request.amount(), request.orderName());
    }

    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("@userScope.matches(authentication, #userId)")
    public TossTestCheckoutService.ConfirmedOrder confirm(
            @PathVariable UUID userId,
            @PathVariable @Size(min = 6, max = 64) String orderId,
            @Valid @RequestBody ConfirmRequest request
    ) {
        return service.confirm(userId, orderId, request.paymentKey(), request.amount());
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("@userScope.matches(authentication, #userId)")
    public TossTestCheckoutService.CanceledOrder cancel(
            @PathVariable UUID userId,
            @PathVariable @Size(min = 6, max = 64) String orderId,
            @Valid @RequestBody CancelRequest request
    ) {
        return service.cancel(userId, orderId, request.paymentKey());
    }

    public record CreateRequest(
            @Min(TossTestCheckoutService.MINIMUM_AMOUNT)
            @Max(TossTestCheckoutService.MAXIMUM_AMOUNT)
            long amount,
            @Size(max = 100) String orderName
    ) {
    }

    public record ConfirmRequest(
            @NotBlank @Size(max = 200) String paymentKey,
            @Min(TossTestCheckoutService.MINIMUM_AMOUNT)
            @Max(TossTestCheckoutService.MAXIMUM_AMOUNT)
            long amount
    ) {
    }

    public record CancelRequest(@NotBlank @Size(max = 200) String paymentKey) {
    }
}
