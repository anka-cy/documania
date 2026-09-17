package com.documania.backend.audit;

import com.documania.backend.audit.dto.AuditResponse;

public final class AuditMapper {

    private AuditMapper() {
    }

    public static AuditResponse toResponse(AuditLog audit) {
        return new AuditResponse(
            audit.getActorEmail(),
            audit.getActorRole(),
            audit.getAction(),
            audit.getEntityType(),
            audit.getEntityPublicId(),
            audit.getEntityLabel(),
            audit.getOldValues(),
            audit.getNewValues(),
            audit.getReason(),
            audit.getCreatedAt()
        );
    }
}
