package com.joajy.spendingguard.account.notification;

import com.joajy.spendingguard.account.service.port.outbound.SendVerificationCodePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 개발 환경에서 외부 메일 사업자 없이 발송 경계를 검증하는 대체 어댑터다. */
@Component
class LoggingVerificationCodeSender implements SendVerificationCodePort {
    private static final Logger log = LoggerFactory.getLogger(LoggingVerificationCodeSender.class);
    public void send(String email, String code) { log.info("Email verification requested for {} (code omitted)", email); }
}
