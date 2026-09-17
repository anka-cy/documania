package com.documania.backend.offer;

import com.documania.backend.offer.dto.OfferResponse;

public final class OfferMapper {

    private OfferMapper() {
    }

    public static OfferResponse toResponse(Offer offer) {
        return new OfferResponse(
            offer.getPublicId(),
            offer.getCatalogService().getPublicId(),
            offer.getCatalogService().getName(),
            offer.getName(),
            offer.getDescription(),
            offer.getPrice(),
            offer.getDurationMonths(),
            offer.getNumberOfUsers(),
            offer.getCommercialStartDate(),
            offer.getCommercialEndDate(),
            offer.isArchived(),
            offer.getArchivedAt(),
            offer.getCreatedAt(),
            offer.getUpdatedAt()
        );
    }
}
