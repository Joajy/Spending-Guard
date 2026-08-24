package com.joajy.spendingguard.auth.controller;

import com.joajy.spendingguard.auth.service.LoginService;
import com.joajy.spendingguard.auth.service.result.AuthTokens;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    private final LoginService service;

    public LoginController(LoginService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokens> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request.email(), request.password()));
    }
}
