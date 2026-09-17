package com.documania.backend.subscription;

import com.documania.backend.order.CustomerOrder;
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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_periods")
public class SubscriptionPeriod {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;
    @Column(name = "period_number", nullable = false) private int periodNumber;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Column(name = "price_snapshot", nullable = false, precision = 12, scale = 2) private BigDecimal priceSnapshot;
    @Column(name = "offer_name_snapshot", nullable = false, length = 100) private String offerNameSnapshot;
    @Column(name = "offer_duration_months_snapshot", nullable = false) private int offerDurationMonthsSnapshot;
    @Column(name = "offer_number_of_users_snapshot", nullable = false) private int offerNumberOfUsersSnapshot;
    @Column(name = "service_name_snapshot", nullable = false, length = 100) private String serviceNameSnapshot;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_user_id") private UserAccount createdBy;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    protected SubscriptionPeriod() {}

    public SubscriptionPeriod(Subscription subscription, CustomerOrder order, UserAccount createdBy,
                              LocalDate startDate, LocalDate endDate) {
        this.subscription = subscription;
        this.periodNumber = 1;
        this.startDate = startDate;
        this.endDate = endDate;
        this.priceSnapshot = order.getPriceSnapshot();
        this.offerNameSnapshot = order.getOfferNameSnapshot();
        this.offerDurationMonthsSnapshot = order.getOfferDurationMonthsSnapshot();
        this.offerNumberOfUsersSnapshot = order.getOfferNumberOfUsersSnapshot();
        this.serviceNameSnapshot = order.getServiceNameSnapshot();
        this.createdBy = createdBy;
    }

    public int getPeriodNumber() { return periodNumber; }
    public Subscription getSubscription() { return subscription; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getPriceSnapshot() { return priceSnapshot; }
    public String getOfferNameSnapshot() { return offerNameSnapshot; }
    public int getOfferDurationMonthsSnapshot() { return offerDurationMonthsSnapshot; }
    public int getOfferNumberOfUsersSnapshot() { return offerNumberOfUsersSnapshot; }
    public String getServiceNameSnapshot() { return serviceNameSnapshot; }
    public UserAccount getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
