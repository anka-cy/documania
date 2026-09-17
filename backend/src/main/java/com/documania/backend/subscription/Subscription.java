package com.documania.backend.subscription;

import com.documania.backend.client.Client;
import com.documania.backend.offer.Offer;
import com.documania.backend.order.CustomerOrder;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private UUID publicId;
    @ManyToOne(fetch = FetchType.LAZY, optional = true) @JoinColumn(name = "client_id")
    private Client client;
    @Column(name = "client_company_name_snapshot", nullable = false, length = 150)
    private String clientCompanyNameSnapshot;
    @ManyToOne(fetch = FetchType.LAZY, optional = true) @JoinColumn(name = "offer_id")
    private Offer offer;
    @Column(name = "offer_name_snapshot", nullable = false, length = 100)
    private String offerNameSnapshot;
    @Column(name = "service_name_snapshot", nullable = false, length = 100)
    private String serviceNameSnapshot;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_order_id", unique = true)
    private CustomerOrder customerOrder;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SubscriptionStatus status;
    @Column(name = "cancellation_reason", columnDefinition = "text") private String cancellationReason;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Column(name = "expiry_reminder_sent", nullable = false) private boolean expiryReminderSent;
    @Column(name = "cancelled_at") private LocalDateTime cancelledAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cancelled_by") private UserAccount cancelledBy;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected Subscription() {}

    public Subscription(Client client, Offer offer, CustomerOrder customerOrder, LocalDate startDate, LocalDate endDate) {
        this.client = client;
        this.clientCompanyNameSnapshot = client.getCompanyName();
        this.offer = offer;
        this.offerNameSnapshot = offer.getName();
        this.serviceNameSnapshot = offer.getCatalogService().getName();
        this.customerOrder = customerOrder;
        this.status = SubscriptionStatus.ACTIVE;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @PrePersist void generatePublicId() { if (publicId == null) publicId = UUID.randomUUID(); }

    public void cancel(String reason, UserAccount administrator, LocalDateTime time) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledBy = administrator;
        this.cancelledAt = time;
    }

    public void markExpiryReminderSent() {
        this.expiryReminderSent = true;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
    }
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Client getClient() { return client; }
    public Offer getOffer() { return offer; }
    public String getOfferNameSnapshot() { return offerNameSnapshot; }
    public String getServiceNameSnapshot() { return serviceNameSnapshot; }
    public CustomerOrder getCustomerOrder() { return customerOrder; }
    public String getClientCompanyNameSnapshot() { return clientCompanyNameSnapshot; }
    public SubscriptionStatus getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isExpiryReminderSent() { return expiryReminderSent; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public UserAccount getCancelledBy() { return cancelledBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
