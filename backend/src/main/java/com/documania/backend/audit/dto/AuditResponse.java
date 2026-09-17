package com.documania.backend.audit.dto;

import com.documania.backend.audit.AuditAction;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditResponse(
    String actorEmail,
    String actorRole,
    AuditAction action,
    String entityType,
    UUID entityPublicId,
    String entityLabel,
    Map<String, Object> oldValues,
    Map<String, Object> newValues,
    String reason,
    LocalDateTime createdAt
) {
}
