package com.documania.backend.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "services")
public class CatalogService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private UUID publicId;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 100)
    private String category;

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

    protected CatalogService() {
    }

    public CatalogService(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.archived = false;
    }

    @PrePersist
    void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }

    public void changeDetails(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
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
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public boolean isArchived() { return archived; }
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
