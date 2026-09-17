package com.documania.backend.catalog;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogServiceTest {

    @Test
    void shouldGeneratePublicIdOnlyOnceBeforeInsert() {
        CatalogService service = newService();

        service.generatePublicId();
        UUID firstPublicId = service.getPublicId();
        service.generatePublicId();

        assertNotNull(firstPublicId);
        assertEquals(firstPublicId, service.getPublicId());
    }

    @Test
    void shouldChangeEditableDetails() {
        CatalogService service = newService();

        service.changeDetails(
            "Cloud Backup Plus",
            "Managed cloud backup",
            "Cloud"
        );

        assertEquals("Cloud Backup Plus", service.getName());
        assertEquals("Managed cloud backup", service.getDescription());
        assertEquals("Cloud", service.getCategory());
    }

    @Test
    void shouldArchiveOnceAndRestore() {
        CatalogService service = newService();

        service.archive();
        LocalDateTime firstArchivedAt = service.getArchivedAt();
        service.archive();

        assertTrue(service.isArchived());
        assertNotNull(firstArchivedAt);
        assertEquals(firstArchivedAt, service.getArchivedAt());

        service.restore();

        assertFalse(service.isArchived());
        assertNull(service.getArchivedAt());
    }

    private CatalogService newService() {
        return new CatalogService(
            "Cloud Backup",
            "Secure company backups",
            "Cloud"
        );
    }
}
