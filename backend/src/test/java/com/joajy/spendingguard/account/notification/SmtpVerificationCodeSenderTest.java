package com.joajy.spendingguard.account.notification;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpVerificationCodeSenderTest {
    @Test
    void sendsCodeToRequestedAddressWithoutChangingIt() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        var sender = new SmtpVerificationCodeSender(mailSender, "no-reply@spending-guard.local");

        sender.send("user@example.com", "004201");

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("user@example.com");
        assertThat(captor.getValue().getFrom()).isEqualTo("no-reply@spending-guard.local");
        assertThat(captor.getValue().getText()).contains("004201").contains("5분");
    }
}
