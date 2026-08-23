package com.joajy.spendingguard.auth.controller;

import com.joajy.spendingguard.auth.service.LoginService;
import com.joajy.spendingguard.auth.service.result.AccessToken;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/login")
public class LoginController {

    private final LoginService service;

    public LoginController(LoginService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AccessToken> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request.email(), request.password()));
    }
}
