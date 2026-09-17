package com.documania.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EmailOutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxScheduler.class);

    private static final int BATCH_SIZE = 50;
    private static final long RUN_INTERVAL_MS = 30000;

    private final EmailOutboxRepository outboxRepository;
    private final EmailService emailService;

    public EmailOutboxScheduler(EmailOutboxRepository outboxRepository, EmailService emailService) {
        this.outboxRepository = outboxRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelay = RUN_INTERVAL_MS)
    public void sendPendingEmails() {
        List<EmailOutbox> due = collectDueRecords();
        for (EmailOutbox record : due) {
            sendRecord(record);
        }
    }

    List<EmailOutbox> collectDueRecords() {
        List<EmailOutbox> due = new ArrayList<>();
        due.addAll(outboxRepository.findAllByStatus(EmailOutboxStatus.PENDING));
        due.addAll(outboxRepository.findAllByStatusAndNextRetryAtLessThanEqual(
            EmailOutboxStatus.FAILED, java.time.LocalDateTime.now()));
        return due.stream().limit(BATCH_SIZE).toList();
    }

    private void sendRecord(EmailOutbox record) {
        if (record.getAttempts() >= record.getMaxAttempts()) {
            log.error("Email outbox record {} reached max attempts ({}) and will not be retried.",
                record.getId(), record.getMaxAttempts());
            return;
        }
        try {
            emailService.send(record.getRecipient(), record.getSubject(), record.getBody());
            record.markSent();
            outboxRepository.save(record);
        } catch (Exception ex) {
            int retryDelayMinutes = Math.min(60, record.getAttempts() * 5 + 5);
            record.markFailed(ex.getMessage(), retryDelayMinutes);
            outboxRepository.save(record);
            log.warn("Email outbox record {} failed (attempt {}/{}): {}",
                record.getId(), record.getAttempts(), record.getMaxAttempts(), ex.getMessage());
        }
    }
}