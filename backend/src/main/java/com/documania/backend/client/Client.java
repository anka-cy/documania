package com.documania.backend.client;

import com.documania.backend.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private UUID publicId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(length = 30)
    private String phone;

    @Column(columnDefinition = "text")
    private String address;

    @Column(length = 100)
    private String sector;

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

    protected Client() {
    }

    public Client(
        UserAccount userAccount,
        String companyName,
        String phone,
        String address,
        String sector
    ) {
        this.userAccount = userAccount;
        this.companyName = companyName;
        this.phone = phone;
        this.address = address;
        this.sector = sector;
        this.archived = false;
    }

    @PrePersist
    void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }

    public void changeDetails(
        String companyName,
        String phone,
        String address,
        String sector
    ) {
        this.companyName = companyName;
        this.phone = phone;
        this.address = address;
        this.sector = sector;
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
    public UserAccount getUserAccount() { return userAccount; }
    public String getCompanyName() { return companyName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getSector() { return sector; }
    public boolean isArchived() { return archived; }
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
