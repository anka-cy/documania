package com.documania.backend.offer;

import com.documania.backend.catalog.CatalogService;
import com.documania.backend.offer.dto.OfferResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OfferMapperTest {

    @Test
    void shouldMapOfferAndParentService() {
        CatalogService service = new CatalogService("Cloud Backup", null, null);
        Offer offer = new Offer(
            service, "Standard", "Base", new BigDecimal("99.90"), 12, 10,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );
        OfferResponse response = OfferMapper.toResponse(offer);
        assertEquals("Cloud Backup", response.serviceName());
        assertEquals("Standard", response.name());
        assertEquals(new BigDecimal("99.90"), response.price());
        assertFalse(response.archived());
    }
}
