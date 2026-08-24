package com.joajy.spendingguard.auth.repository;

import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.account.repository.UserAccountJpaRepository;
import com.joajy.spendingguard.auth.service.model.LoginAccount;
import com.joajy.spendingguard.auth.service.port.LoadLoginAccountPort;
import org.springframework.stereotype.Component;

@Component
class LoginAccountPersistenceAdapter implements LoadLoginAccountPort {

    private final UserAccountJpaRepository repository;

    LoginAccountPersistenceAdapter(UserAccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<LoginAccount> findByEmail(String normalizedEmail) {
        return repository.findByEmail(normalizedEmail).map(this::toLoginAccount);
    }

    @Override
    public Optional<LoginAccount> findById(UUID userId) {
        return repository.findById(userId).map(this::toLoginAccount);
    }

    private LoginAccount toLoginAccount(com.joajy.spendingguard.account.repository.UserAccountEntity account) {
        return new LoginAccount(
                account.getId(),
                account.getEmail(),
                account.getPasswordHash(),
                account.getEmailVerifiedAt() != null
        );
    }
}
