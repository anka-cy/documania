package com.documania.backend.notification;

import com.documania.backend.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserAccount recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, updatable = false)
    private NotificationType type;

    @Column(nullable = false, length = 200) private String title;

    @Column(nullable = false, length = 500) private String message;

    @Column(length = 255) private String link;

    @Column(name = "read_at") private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Notification() {}

    public Notification(
        UserAccount recipient,
        NotificationType type,
        String title,
        String message,
        String link
    ) {
        this.publicId = UUID.randomUUID();
        this.recipient = recipient;
        this.type = type;
        this.title = title;
        this.message = message;
        this.link = link;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }

    public boolean isRead() {
        return readAt != null;
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public UserAccount getRecipient() { return recipient; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getLink() { return link; }
    public LocalDateTime getReadAt() { return readAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
