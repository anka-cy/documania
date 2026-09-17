package com.documania.backend.order;

import com.documania.backend.client.Client;
import com.documania.backend.offer.Offer;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private UUID publicId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "client_id") private Client client;
    @ManyToOne(fetch = FetchType.LAZY, optional = true) @JoinColumn(name = "offer_id")
    private Offer offer;
    @Column(name = "order_number", nullable = false, unique = true, length = 30) private String orderNumber;
    @Column(name = "client_company_name_snapshot", nullable = false, length = 150) private String clientCompanyNameSnapshot;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OrderStatus status;
    @Column(name = "rejection_reason", columnDefinition = "text") private String rejectionReason;
    @Column(name = "price_snapshot", nullable = false, precision = 12, scale = 2) private BigDecimal priceSnapshot;
    @Column(name = "offer_name_snapshot", nullable = false, length = 100) private String offerNameSnapshot;
    @Column(name = "offer_duration_months_snapshot", nullable = false) private int offerDurationMonthsSnapshot;
    @Column(name = "offer_number_of_users_snapshot", nullable = false) private int offerNumberOfUsersSnapshot;
    @Column(name = "service_name_snapshot", nullable = false, length = 100) private String serviceNameSnapshot;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "processed_by_user_id") private UserAccount processedBy;
    @Column(name = "processed_at") private LocalDateTime processedAt;
    @Column(name = "cancelled_at") private LocalDateTime cancelledAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected CustomerOrder() {}

    public CustomerOrder(Client client, Offer offer, String orderNumber) {
        this.client = client;
        this.offer = offer;
        this.orderNumber = orderNumber;
        this.clientCompanyNameSnapshot = client.getCompanyName();
        this.status = OrderStatus.PENDING;
        this.priceSnapshot = offer.getPrice();
        this.offerNameSnapshot = offer.getName();
        this.offerDurationMonthsSnapshot = offer.getDurationMonths();
        this.offerNumberOfUsersSnapshot = offer.getNumberOfUsers();
        this.serviceNameSnapshot = offer.getCatalogService().getName();
    }

    @PrePersist void generatePublicId() { if (publicId == null) publicId = UUID.randomUUID(); }

    public void cancel(LocalDateTime time) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = time;
    }

    public void reject(String reason, UserAccount administrator, LocalDateTime time) {
        this.status = OrderStatus.REJECTED;
        this.rejectionReason = reason;
        this.processedBy = administrator;
        this.processedAt = time;
    }

    public void confirm(UserAccount administrator, LocalDateTime time) {
        this.status = OrderStatus.CONFIRMED;
        this.processedBy = administrator;
        this.processedAt = time;
    }
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Client getClient() { return client; }
    public Offer getOffer() { return offer; }
    public String getOrderNumber() { return orderNumber; }
    public String getClientCompanyNameSnapshot() { return clientCompanyNameSnapshot; }
    public OrderStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public BigDecimal getPriceSnapshot() { return priceSnapshot; }
    public String getOfferNameSnapshot() { return offerNameSnapshot; }
    public int getOfferDurationMonthsSnapshot() { return offerDurationMonthsSnapshot; }
    public int getOfferNumberOfUsersSnapshot() { return offerNumberOfUsersSnapshot; }
    public String getServiceNameSnapshot() { return serviceNameSnapshot; }
    public UserAccount getProcessedBy() { return processedBy; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
