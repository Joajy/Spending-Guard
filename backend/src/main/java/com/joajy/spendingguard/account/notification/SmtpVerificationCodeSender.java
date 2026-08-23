package com.joajy.spendingguard.account.notification;

import com.joajy.spendingguard.account.service.port.outbound.SendVerificationCodePort;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** 인증번호를 SMTP로 전달하며 메일 본문 외의 로그에는 인증번호를 남기지 않는다. */
@Component
class SmtpVerificationCodeSender implements SendVerificationCodePort {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String from;

    SmtpVerificationCodeSender(JavaMailSender mailSender, TemplateEngine templateEngine,
                               @Value("${spending-guard.mail.from}") String from) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.from = from;
    }

    @Override
    public void send(String email, String code) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("Spending Guard 이메일 인증번호");
            helper.setText(render(code), true);
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new MailPreparationException("Could not prepare verification email", exception);
        }
    }

    private String render(String code) {
        var context = new Context();
        context.setVariable("verificationCode", code);
        context.setVariable("validMinutes", 5);
        return templateEngine.process("mail/email-verification", context);
    }
}
