package com.documania.backend.offer;

import com.documania.backend.catalog.CatalogService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "offers")
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, updatable = false)
    private CatalogService catalogService;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_months", nullable = false)
    private int durationMonths;

    @Column(name = "number_of_users", nullable = false)
    private int numberOfUsers;

    @Column(name = "commercial_start_date", nullable = false)
    private LocalDate commercialStartDate;

    @Column(name = "commercial_end_date", nullable = false)
    private LocalDate commercialEndDate;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Offer() {
    }

    public Offer(
        CatalogService catalogService,
        String name,
        String description,
        BigDecimal price,
        int durationMonths,
        int numberOfUsers,
        LocalDate commercialStartDate,
        LocalDate commercialEndDate
    ) {
        this.catalogService = catalogService;
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationMonths = durationMonths;
        this.numberOfUsers = numberOfUsers;
        this.commercialStartDate = commercialStartDate;
        this.commercialEndDate = commercialEndDate;
        this.archived = false;
    }

    @PrePersist
    void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }

    public void changeDetails(
        String name,
        String description,
        BigDecimal price,
        int durationMonths,
        int numberOfUsers,
        LocalDate commercialStartDate,
        LocalDate commercialEndDate
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationMonths = durationMonths;
        this.numberOfUsers = numberOfUsers;
        this.commercialStartDate = commercialStartDate;
        this.commercialEndDate = commercialEndDate;
    }

    public void archive() {
        if (!archived) {
            archived = true;
            archivedAt = LocalDateTime.now();
        }
    }

    public void restore() {
        archived = false;
        archivedAt = null;
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public CatalogService getCatalogService() { return catalogService; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public int getDurationMonths() { return durationMonths; }
    public int getNumberOfUsers() { return numberOfUsers; }
    public LocalDate getCommercialStartDate() { return commercialStartDate; }
    public LocalDate getCommercialEndDate() { return commercialEndDate; }
    public boolean isArchived() { return archived; }
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
