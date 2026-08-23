package com.joajy.spendingguard.account.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpVerificationCodeSenderTest {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final TemplateEngine templateEngine = mock(TemplateEngine.class);

    @Test
    void sendsRenderedHtmlToRequestedAddress() throws Exception {
        var message = new MimeMessage((Session) null);
        given(mailSender.createMimeMessage()).willReturn(message);
        given(templateEngine.process(eq("mail/email-verification"), any(Context.class)))
                .willReturn("<strong>004201</strong>");
        var sender = new SmtpVerificationCodeSender(mailSender, templateEngine,
                "no-reply@spending-guard.local");

        sender.send("user@example.com", "004201");

        var context = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mail/email-verification"), context.capture());
        verify(mailSender).send(message);
        message.saveChanges();
        assertThat(context.getValue().getVariable("verificationCode")).isEqualTo("004201");
        assertThat(context.getValue().getVariable("validMinutes")).isEqualTo(5);
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("user@example.com");
        assertThat(message.getSubject()).isEqualTo("Spending Guard 이메일 인증번호");
        assertThat(message.getContentType()).startsWith("text/html");
        assertThat(message.getContent().toString()).contains("004201");
    }

    @Test
    void exposesDeliveryFailureToTheCaller() {
        var message = new MimeMessage((Session) null);
        given(mailSender.createMimeMessage()).willReturn(message);
        given(templateEngine.process(eq("mail/email-verification"), any(Context.class)))
                .willReturn("<strong>004201</strong>");
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(message);
        var sender = new SmtpVerificationCodeSender(mailSender, templateEngine,
                "no-reply@spending-guard.local");

        assertThatThrownBy(() -> sender.send("user@example.com", "004201"))
                .isInstanceOf(MailSendException.class);
    }
}
