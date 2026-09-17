package com.documania.backend.email;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_outbox")
public class EmailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String recipient;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailOutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EmailOutbox() {
    }

    public EmailOutbox(String recipient, String subject, String body) {
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.status = EmailOutboxStatus.PENDING;
        this.attempts = 0;
        this.maxAttempts = 5;
    }

    public void markSent() {
        this.status = EmailOutboxStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.lastError = null;
        this.nextRetryAt = null;
    }

    public void markFailed(String errorMessage, int retryDelayMinutes) {
        this.attempts++;
        this.status = EmailOutboxStatus.FAILED;
        this.lastError = errorMessage;
        if (this.attempts < this.maxAttempts) {
            this.nextRetryAt = LocalDateTime.now().plusMinutes(retryDelayMinutes);
        } else {
            this.nextRetryAt = null;
        }
    }

    public Long getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public EmailOutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}