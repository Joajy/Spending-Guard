package com.joajy.spendingguard.account.notification;

import com.joajy.spendingguard.account.service.port.outbound.SendVerificationCodePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** 인증번호를 SMTP로 전달하며 메일 본문 외의 로그에는 인증번호를 남기지 않는다. */
@Component
class SmtpVerificationCodeSender implements SendVerificationCodePort {
    private final JavaMailSender mailSender;
    private final String from;

    SmtpVerificationCodeSender(JavaMailSender mailSender,
                               @Value("${spending-guard.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Spending Guard 이메일 인증번호");
        message.setText("이메일 인증번호는 " + code + "입니다. 5분 안에 입력해 주세요.");
        mailSender.send(message);
    }
}
