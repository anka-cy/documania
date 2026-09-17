package com.documania.backend.catalog;

import com.documania.backend.catalog.dto.CatalogServiceResponse;

public final class CatalogServiceMapper {

    private CatalogServiceMapper() {
    }

    public static CatalogServiceResponse toResponse(CatalogService service) {
        return new CatalogServiceResponse(
            service.getPublicId(),
            service.getName(),
            service.getDescription(),
            service.getCategory(),
            service.isArchived(),
            service.getArchivedAt(),
            service.getCreatedAt(),
            service.getUpdatedAt()
        );
    }
}
