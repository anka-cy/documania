package com.documania.backend.offer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record OfferResponse(
    UUID publicId,
    UUID servicePublicId,
    String serviceName,
    String name,
    String description,
    BigDecimal price,
    int durationMonths,
    int numberOfUsers,
    LocalDate commercialStartDate,
    LocalDate commercialEndDate,
    boolean archived,
    LocalDateTime archivedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
