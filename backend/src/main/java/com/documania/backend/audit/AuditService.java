package com.documania.backend.audit;

import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserAccountRepository userAccountRepository;

    public AuditService(
        AuditLogRepository auditLogRepository,
        UserAccountRepository userAccountRepository
    ) {
        this.auditLogRepository = auditLogRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public void record(
        AuditAction action,
        String entityType,
        Long entityId,
        UUID entityPublicId,
        String entityLabel,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        String reason
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Une action métier auditée exige un utilisateur authentifié");
        }

        String actorEmail = authentication.getName();
        UserAccount actor = userAccountRepository.findByEmailIgnoreCase(actorEmail)
            .orElseThrow(() -> new IllegalStateException("Utilisateur d'audit introuvable"));

        AuditLog auditLog = new AuditLog(
            actor,
            actorEmail,
            findRole(authentication),
            action,
            entityType,
            entityId,
            entityPublicId,
            entityLabel,
            emptyToNull(oldValues),
            emptyToNull(newValues),
            reason
        );

        auditLogRepository.save(auditLog);
    }

    public void recordSystem(
        AuditAction action,
        String entityType,
        Long entityId,
        UUID entityPublicId,
        String entityLabel,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        String reason
    ) {
        AuditLog auditLog = new AuditLog(
            null,
            "SYSTEM",
            "SYSTEM",
            action,
            entityType,
            entityId,
            entityPublicId,
            entityLabel,
            emptyToNull(oldValues),
            emptyToNull(newValues),
            reason
        );
        auditLogRepository.save(auditLog);
    }

    private String findRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring("ROLE_".length()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Rôle d'audit introuvable"));
    }

    private Map<String, Object> emptyToNull(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
