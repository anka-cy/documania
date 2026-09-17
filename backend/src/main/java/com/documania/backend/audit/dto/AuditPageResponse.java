package com.documania.backend.audit.dto;

import java.util.List;

public record AuditPageResponse(
    List<AuditResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
