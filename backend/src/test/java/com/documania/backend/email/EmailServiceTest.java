package com.documania.backend.email;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceTest {

    @Test
    void shouldBuildPlainTextActivationMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(mailSender, "no-reply@documania.test", "https://portal.documania.test/");
        LocalDateTime expiresAt = LocalDateTime.of(2026, 8, 15, 14, 30);

        EmailService.EmailMessage message =
            service.buildAccountActivationMessage("client@test.local", "safe-token", expiresAt);

        assertEquals("client@test.local", message.recipient());
        assertEquals("Activez votre compte Documania", message.subject());
        assertTrue(message.body().contains("https://portal.documania.test/#/activate-account?token=safe-token"));
        assertTrue(message.body().contains("15/08/2026 14:30"));
    }

    @Test
    void shouldBuildPlainTextPasswordResetMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(mailSender, "no-reply@documania.test", "https://portal.documania.test/");
        LocalDateTime expiresAt = LocalDateTime.of(2026, 8, 15, 14, 30);

        EmailService.EmailMessage message =
            service.buildPasswordResetMessage("client@test.local", "reset-token", expiresAt);

        assertEquals("client@test.local", message.recipient());
        assertEquals("Réinitialisez votre mot de passe Documania", message.subject());
        assertTrue(message.body().contains("https://portal.documania.test/#/password-reset-confirm?token=reset-token"));
        assertTrue(message.body().contains("15/08/2026 14:30"));
    }

    @Test
    void shouldSendRenderedMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(mailSender, "no-reply@documania.test", "https://portal.documania.test/");

        service.send("client@test.local", "Sujet", "Corps");

        var captor = forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertEquals("no-reply@documania.test", sent.getFrom());
        assertEquals("client@test.local", sent.getTo()[0]);
        assertEquals("Sujet", sent.getSubject());
        assertEquals("Corps", sent.getText());
    }
}