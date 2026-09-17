package com.documania.backend.catalog.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CatalogServiceResponse(
    UUID publicId,
    String name,
    String description,
    String category,
    boolean archived,
    LocalDateTime archivedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
