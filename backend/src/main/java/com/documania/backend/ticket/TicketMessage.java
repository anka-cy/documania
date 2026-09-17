package com.documania.backend.ticket;

import com.documania.backend.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_messages")
public class TicketMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private UUID publicId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;
    @Column(columnDefinition = "text", nullable = false) private String message;
    @Column(name = "is_internal", nullable = false) private boolean internal;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    protected TicketMessage() {}

    public TicketMessage(Ticket ticket, UserAccount author, String message, boolean internal) {
        this.ticket = ticket;
        this.author = author;
        this.message = message;
        this.internal = internal;
    }

    @PrePersist void generatePublicId() { if (publicId == null) publicId = UUID.randomUUID(); }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Ticket getTicket() { return ticket; }
    public UserAccount getAuthor() { return author; }
    public String getMessage() { return message; }
    public boolean isInternal() { return internal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
