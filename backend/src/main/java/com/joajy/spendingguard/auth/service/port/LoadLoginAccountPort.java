package com.joajy.spendingguard.auth.service.port;

import java.util.Optional;

import com.joajy.spendingguard.auth.service.model.LoginAccount;

public interface LoadLoginAccountPort {
    Optional<LoginAccount> findByEmail(String normalizedEmail);

    Optional<LoginAccount> findById(java.util.UUID userId);
}
