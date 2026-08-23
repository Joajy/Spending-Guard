package com.joajy.spendingguard.account.service.port.outbound;

public interface SendVerificationCodePort {
    void send(String email, String code);
}
