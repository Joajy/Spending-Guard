package com.joajy.spendingguard.spendevent.repository;

import java.util.UUID;
import com.joajy.spendingguard.account.repository.UserAccountJpaRepository;
import com.joajy.spendingguard.spendevent.service.port.outbound.CheckSpendEventOwnerPort;
import org.springframework.stereotype.Component;

@Component
class SpendEventOwnerPersistenceAdapter implements CheckSpendEventOwnerPort {
    private final UserAccountJpaRepository accounts;
    SpendEventOwnerPersistenceAdapter(UserAccountJpaRepository accounts) { this.accounts = accounts; }
    public boolean exists(UUID userId) { return userId != null && accounts.existsById(userId); }
}
