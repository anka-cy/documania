package com.documania.backend.email;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmailOutboxTest {

    @Test
    void shouldStartAsPendingWithZeroAttemptsAndMaxFive() {
        EmailOutbox outbox = new EmailOutbox("client@test.local", "Sujet", "Corps");

        assertEquals("client@test.local", outbox.getRecipient());
        assertEquals("Sujet", outbox.getSubject());
        assertEquals("Corps", outbox.getBody());
        assertEquals(EmailOutboxStatus.PENDING, outbox.getStatus());
        assertEquals(0, outbox.getAttempts());
        assertEquals(5, outbox.getMaxAttempts());
    }

    @Test
    void shouldMarkRecordSentWithTimestamp() {
        EmailOutbox outbox = new EmailOutbox("client@test.local", "Sujet", "Corps");

        outbox.markSent();

        assertEquals(EmailOutboxStatus.SENT, outbox.getStatus());
        assertNotNull(outbox.getSentAt());
        assertNull(outbox.getNextRetryAt());
    }

    @Test
    void shouldStopSchedulingRetriesAfterMaxAttempts() {
        EmailOutbox outbox = new EmailOutbox("client@test.local", "Sujet", "Corps");

        for (int i = 0; i < outbox.getMaxAttempts(); i++) {
            outbox.markFailed("issue", 5);
        }

        assertEquals(EmailOutboxStatus.FAILED, outbox.getStatus());
        assertEquals(outbox.getMaxAttempts(), outbox.getAttempts());
        assertNull(outbox.getNextRetryAt());
    }
}
