package com.documania.backend.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CatalogueResponse(
    UUID publicId,
    String name,
    String description,
    String category,
    java.util.List<CatalogueOfferResponse> offers
) {
    public record CatalogueOfferResponse(
        UUID publicId,
        String name,
        String description,
        BigDecimal price,
        int durationMonths,
        int numberOfUsers,
        LocalDate commercialStartDate,
        LocalDate commercialEndDate
    ) {
    }
}