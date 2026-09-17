package com.documania.backend.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailOutboxSchedulerTest {

    private EmailOutboxRepository outboxRepository;
    private EmailService emailService;
    private EmailOutboxScheduler scheduler;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(EmailOutboxRepository.class);
        emailService = mock(EmailService.class);
        scheduler = new EmailOutboxScheduler(outboxRepository, emailService);
        when(outboxRepository.findAllByStatus(EmailOutboxStatus.PENDING)).thenReturn(List.of());
        when(outboxRepository.findAllByStatusAndNextRetryAtLessThanEqual(eq(EmailOutboxStatus.FAILED), any()))
            .thenReturn(List.of());
    }

    @Test
    void shouldSendPendingRecordsAndMarkThemSent() {
        EmailOutbox record = new EmailOutbox("client@test.local", "Sujet", "Corps");
        when(outboxRepository.findAllByStatus(EmailOutboxStatus.PENDING)).thenReturn(List.of(record));

        scheduler.sendPendingEmails();

        verify(emailService).send("client@test.local", "Sujet", "Corps");
        ArgumentCaptor<EmailOutbox> captor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(outboxRepository).save(captor.capture());
        EmailOutbox saved = captor.getValue();
        assertEquals(EmailOutboxStatus.SENT, saved.getStatus());
        assertNotNull(saved.getSentAt());
    }

    @Test
    void shouldMarkFailedRecordsWithLastErrorAndRetryDelay() {
        EmailOutbox record = new EmailOutbox("client@test.local", "Sujet", "Corps");
        when(outboxRepository.findAllByStatus(EmailOutboxStatus.PENDING)).thenReturn(List.of(record));
        doThrow(new RuntimeException("SMTP down")).when(emailService).send(any(), any(), any());

        scheduler.sendPendingEmails();

        ArgumentCaptor<EmailOutbox> captor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(outboxRepository).save(captor.capture());
        EmailOutbox saved = captor.getValue();
        assertEquals(EmailOutboxStatus.FAILED, saved.getStatus());
        assertEquals(1, saved.getAttempts());
        assertEquals("SMTP down", saved.getLastError());
        assertNotNull(saved.getNextRetryAt());
        assertTrue(saved.getNextRetryAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void shouldRetryFailedRecordsWhoseRetryIsDue() {
        EmailOutbox record = new EmailOutbox("client@test.local", "Sujet", "Corps");
        record.markFailed("temporary issue", 5);
        when(outboxRepository.findAllByStatusAndNextRetryAtLessThanEqual(eq(EmailOutboxStatus.FAILED), any()))
            .thenReturn(List.of(record));

        scheduler.sendPendingEmails();

        verify(emailService).send("client@test.local", "Sujet", "Corps");
        verify(outboxRepository).save(any(EmailOutbox.class));
    }

    @Test
    void shouldStopRetryingWhenMaxAttemptsReached() {
        EmailOutbox record = new EmailOutbox("client@test.local", "Sujet", "Corps");
        for (int i = 0; i < record.getMaxAttempts(); i++) {
            record.markFailed("issue", 5);
        }
        when(outboxRepository.findAllByStatus(EmailOutboxStatus.PENDING)).thenReturn(List.of(record));

        scheduler.sendPendingEmails();

        verify(emailService, never()).send(any(), any(), any());
        verify(outboxRepository, never()).save(any(EmailOutbox.class));
    }
}