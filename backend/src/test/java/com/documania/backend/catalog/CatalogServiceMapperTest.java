package com.documania.backend.catalog;

import com.documania.backend.catalog.dto.CatalogServiceResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CatalogServiceMapperTest {

    @Test
    void shouldMapEntityToApiResponse() {
        CatalogService service = new CatalogService(
            "Cloud Backup", "Secure backups", "Cloud"
        );

        CatalogServiceResponse response = CatalogServiceMapper.toResponse(service);

        assertEquals("Cloud Backup", response.name());
        assertEquals("Secure backups", response.description());
        assertEquals("Cloud", response.category());
        assertFalse(response.archived());
    }
}
