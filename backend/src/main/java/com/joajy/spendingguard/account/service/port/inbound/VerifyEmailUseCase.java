package com.joajy.spendingguard.account.service.port.inbound;

import java.util.UUID;

public interface VerifyEmailUseCase {
    void issue(UUID userId);

    void confirm(UUID userId, String code);
}
