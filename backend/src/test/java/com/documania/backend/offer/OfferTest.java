package com.documania.backend.offer;

import com.documania.backend.catalog.CatalogService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferTest {

    @Test
    void shouldGeneratePublicIdOnlyOnce() {
        Offer offer = offer();
        offer.generatePublicId();
        UUID first = offer.getPublicId();
        offer.generatePublicId();
        assertNotNull(first);
        assertEquals(first, offer.getPublicId());
    }

    @Test
    void shouldChangeCommercialDetails() {
        Offer offer = offer();
        offer.changeDetails(
            "Premium", "Updated", new BigDecimal("199.99"), 24, 50,
            LocalDate.of(2026, 2, 1), LocalDate.of(2027, 1, 31)
        );
        assertEquals("Premium", offer.getName());
        assertEquals(new BigDecimal("199.99"), offer.getPrice());
        assertEquals(24, offer.getDurationMonths());
        assertEquals(50, offer.getNumberOfUsers());
    }

    @Test
    void shouldArchiveOnceAndRestore() {
        Offer offer = offer();
        offer.archive();
        LocalDateTime first = offer.getArchivedAt();
        offer.archive();
        assertTrue(offer.isArchived());
        assertEquals(first, offer.getArchivedAt());
        offer.restore();
        assertFalse(offer.isArchived());
        assertNull(offer.getArchivedAt());
    }

    private Offer offer() {
        return new Offer(
            new CatalogService("Cloud", null, null), "Standard", null,
            new BigDecimal("99.90"), 12, 10,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );
    }
}
