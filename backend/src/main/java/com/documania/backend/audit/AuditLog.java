package com.documania.backend.audit;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private UserAccount actor;

    @Column(name = "actor_email", nullable = false, length = 255, updatable = false)
    private String actorEmail;

    @Column(name = "actor_role", nullable = false, length = 50, updatable = false)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100, updatable = false)
    private AuditAction action;

    @Column(name = "entity_type", nullable = false, length = 50, updatable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private Long entityId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "entity_public_id", length = 36, updatable = false)
    private UUID entityPublicId;

    @Column(name = "entity_label", length = 150, updatable = false)
    private String entityLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "json", updatable = false)
    private Map<String, Object> oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "json", updatable = false)
    private Map<String, Object> newValues;

    @Column(columnDefinition = "text", updatable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(
        UserAccount actor,
        String actorEmail,
        String actorRole,
        AuditAction action,
        String entityType,
        Long entityId,
        UUID entityPublicId,
        String entityLabel,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        String reason
    ) {
        this.actor = actor;
        this.actorEmail = actorEmail;
        this.actorRole = actorRole;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityPublicId = entityPublicId;
        this.entityLabel = entityLabel;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.reason = reason;
    }

    public UserAccount getActor() { return actor; }
    public String getActorEmail() { return actorEmail; }
    public String getActorRole() { return actorRole; }
    public AuditAction getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public UUID getEntityPublicId() { return entityPublicId; }
    public String getEntityLabel() { return entityLabel; }
    public Map<String, Object> getOldValues() { return oldValues; }
    public Map<String, Object> getNewValues() { return newValues; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
