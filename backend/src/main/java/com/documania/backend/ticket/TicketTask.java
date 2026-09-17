package com.documania.backend.ticket;

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
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_tasks")
public class TicketTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private UUID publicId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
    @Column(nullable = false, length = 255) private String title;
    @Column(columnDefinition = "text") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TicketTaskStatus status;
    @Column(nullable = false) private int position;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected TicketTask() {}

    public TicketTask(Ticket ticket, String title, String description, int position) {
        this.ticket = ticket;
        this.title = title;
        this.description = description;
        this.status = TicketTaskStatus.PENDING;
        this.position = position;
    }

    @PrePersist void generatePublicId() { if (publicId == null) publicId = UUID.randomUUID(); }

    public void changeStatus(TicketTaskStatus newStatus, LocalDateTime time) {
        this.status = newStatus;
        this.completedAt = newStatus == TicketTaskStatus.COMPLETED ? time : null;
    }

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Ticket getTicket() { return ticket; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TicketTaskStatus getStatus() { return status; }
    public int getPosition() { return position; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
