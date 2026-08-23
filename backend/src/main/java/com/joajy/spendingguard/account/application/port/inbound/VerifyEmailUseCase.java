package com.joajy.spendingguard.account.application.port.inbound;

import java.util.UUID;

public interface VerifyEmailUseCase {
    void issue(UUID userId);

    void confirm(UUID userId, String code);
}
