package com.documania.backend.audit;

import com.documania.backend.audit.dto.AuditPageResponse;
import com.documania.backend.audit.dto.AuditResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;

    public AuditQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public AuditPageResponse search(String query, AuditAction action, String entityType, int page, int size) {
        Page<AuditLog> result = auditLogRepository.search(
            normalize(query), action, normalize(entityType), PageRequest.of(page, size)
        );
        List<AuditResponse> items = result.getContent().stream()
            .map(AuditMapper::toResponse)
            .toList();

        return new AuditPageResponse(
            items,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
