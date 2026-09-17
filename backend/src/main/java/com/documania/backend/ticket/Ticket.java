package com.documania.backend.ticket;

import com.documania.backend.client.Client;
import com.documania.backend.order.CustomerOrder;
import com.documania.backend.subscription.Subscription;
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
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private UUID publicId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "client_id") private Client client;
    @Column(name = "client_company_name_snapshot", nullable = false, length = 255)
    private String clientCompanyNameSnapshot;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id") private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "subscription_id") private Subscription subscription;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_to_user_id") private UserAccount assignedTo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50) private TicketCategory category;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TicketPriority priority;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TicketStatus status;
    @Column(nullable = false, length = 255) private String subject;
    @Column(columnDefinition = "text", nullable = false) private String description;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @Column(name = "closed_at") private LocalDateTime closedAt;

    protected Ticket() {}

    public Ticket(Client client, CustomerOrder order, Subscription subscription,
                  TicketCategory category, TicketPriority priority, String subject, String description) {
        this.client = client;
        this.clientCompanyNameSnapshot = client.getCompanyName();
        this.order = order;
        this.subscription = subscription;
        this.category = category;
        this.priority = priority;
        this.status = TicketStatus.OPEN;
        this.subject = subject;
        this.description = description;
    }

    @PrePersist void generatePublicId() { if (publicId == null) publicId = UUID.randomUUID(); }

    public void changeStatus(TicketStatus newStatus, LocalDateTime time) {
        this.status = newStatus;
        if (newStatus == TicketStatus.CLOSED) {
            this.closedAt = time;
        }
    }

    public void changePriority(TicketPriority newPriority) {
        this.priority = newPriority;
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Client getClient() { return client; }
    public String getClientCompanyNameSnapshot() { return clientCompanyNameSnapshot; }
    public CustomerOrder getOrder() { return order; }
    public Subscription getSubscription() { return subscription; }
    public UserAccount getAssignedTo() { return assignedTo; }
    public TicketCategory getCategory() { return category; }
    public TicketPriority getPriority() { return priority; }
    public TicketStatus getStatus() { return status; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
}
