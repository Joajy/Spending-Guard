package com.joajy.spendingguard.account.application.port.outbound;

public interface SendVerificationCodePort {
    void send(String email, String code);
}
