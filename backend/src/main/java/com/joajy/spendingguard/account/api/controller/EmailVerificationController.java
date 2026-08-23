package com.joajy.spendingguard.account.api.controller;

import java.util.UUID;
import com.joajy.spendingguard.account.api.dto.request.ConfirmEmailRequest;
import com.joajy.spendingguard.account.application.port.inbound.VerifyEmailUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}/email-verification")
public class EmailVerificationController {
    private final VerifyEmailUseCase useCase;
    EmailVerificationController(VerifyEmailUseCase useCase) { this.useCase = useCase; }
    @PostMapping ResponseEntity<Void> issue(@PathVariable UUID userId) { useCase.issue(userId); return ResponseEntity.accepted().build(); }
    @PostMapping("/confirmation") ResponseEntity<Void> confirm(@PathVariable UUID userId, @Valid @RequestBody ConfirmEmailRequest request) {
        useCase.confirm(userId, request.code()); return ResponseEntity.noContent().build();
    }
}
