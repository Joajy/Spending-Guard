package com.joajy.spendingguard.account.controller;

import java.net.URI;

import com.joajy.spendingguard.account.controller.dto.request.RegisterUserRequest;
import com.joajy.spendingguard.account.controller.dto.response.UserRegisteredResponse;
import com.joajy.spendingguard.account.service.port.inbound.RegisterUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** 회원 등록 HTTP 요청을 검증하고 계정 유스케이스에 전달하는 입력 어댑터다. */
@RestController
@RequestMapping("/api/v1/users")
public class UserRegistrationController {

    private final RegisterUserUseCase registerUserUseCase;

    public UserRegistrationController(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }

    @PostMapping
    public ResponseEntity<UserRegisteredResponse> register(
            @Valid @RequestBody RegisterUserRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        UserRegisteredResponse response = UserRegisteredResponse.from(
                registerUserUseCase.register(request.toCommand())
        );
        URI location = uriBuilder.path("/api/v1/users/{userId}").build(response.userId());
        return ResponseEntity.created(location).body(response);
    }
}
