package com.documania.backend.offer;

import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.catalog.CatalogService;
import com.documania.backend.catalog.CatalogServiceRepository;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.offer.dto.SaveOfferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OfferManagementServiceTest {

    private OfferRepository offerRepository;
    private CatalogServiceRepository serviceRepository;
    private AuditService auditService;
    private OfferManagementService management;

    @BeforeEach
    void setUp() {
        offerRepository = mock(OfferRepository.class);
        serviceRepository = mock(CatalogServiceRepository.class);
        auditService = mock(AuditService.class);
        management = new OfferManagementService(
            offerRepository, serviceRepository, auditService
        );
    }

    @Test
    void shouldCreateNormalizedOfferForActiveServiceAndAudit() {
        CatalogService service = service();
        when(serviceRepository.findByPublicIdAndArchived(serviceId(), false))
            .thenReturn(Optional.of(service));
        when(offerRepository.save(any(Offer.class))).thenAnswer(i -> i.getArgument(0));

        Offer result = management.createOffer(serviceId(), createRequest());

        assertEquals("Standard", result.getName());
        assertSame(service, result.getCatalogService());
        verify(auditService).record(
            eq(AuditAction.OFFER_CREATED), eq("OFFER"),
            org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
            eq("Standard"), anyMap(), anyMap(), org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void shouldRejectCreationForMissingOrArchivedService() {
        when(serviceRepository.findByPublicIdAndArchived(serviceId(), false))
            .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
            management.createOffer(serviceId(), createRequest())
        );
        verifyNoInteractions(offerRepository, auditService);
    }

    @Test
    void shouldRejectCommercialEndBeforeStart() {
        when(serviceRepository.findByPublicIdAndArchived(serviceId(), false))
            .thenReturn(Optional.of(service()));
        SaveOfferRequest invalid = new SaveOfferRequest(
            "Standard", null, new BigDecimal("99.90"), 12, 10,
            LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 31)
        );
        assertThrows(BusinessRuleException.class, () ->
            management.createOffer(serviceId(), invalid)
        );
        verify(offerRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateNameWithinService() {
        CatalogService service = service();
        when(serviceRepository.findByPublicIdAndArchived(serviceId(), false))
            .thenReturn(Optional.of(service));
        when(offerRepository.existsByCatalogService_IdAndName(null, "Standard"))
            .thenReturn(true);
        assertThrows(BusinessRuleException.class, () ->
            management.createOffer(serviceId(), createRequest())
        );
        verify(offerRepository, never()).save(any());
    }

    @Test
    void shouldListActiveOffersOfActiveService() {
        CatalogService service = service();
        List<Offer> expected = List.of(offer(service));
        when(serviceRepository.findByPublicIdAndArchived(serviceId(), false))
            .thenReturn(Optional.of(service));
        when(offerRepository.findAllByCatalogService_IdAndArchivedOrderByCreatedAtDesc(null, false))
            .thenReturn(expected);
        assertSame(expected, management.listActiveOffers(serviceId()));
    }

    @Test
    void shouldRejectActiveOfferWhoseServiceIsArchived() {
        CatalogService service = service();
        service.archive();
        Offer offer = offer(service);
        when(offerRepository.findByPublicIdAndArchived(offerId(), false))
            .thenReturn(Optional.of(offer));
        assertThrows(ResourceNotFoundException.class, () -> management.findActiveOffer(offerId()));
    }

    @Test
    void shouldUpdateOfferAndAudit() {
        Offer offer = offer(service());
        when(offerRepository.findByPublicIdAndArchived(offerId(), false))
            .thenReturn(Optional.of(offer));
        when(offerRepository.save(offer)).thenReturn(offer);
        SaveOfferRequest request = new SaveOfferRequest(
            " Premium ", " Updated ", new BigDecimal("149.90"), 24, 25,
            LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31)
        );

        Offer result = management.updateOffer(offerId(), request);

        assertEquals("Premium", result.getName());
        assertEquals(new BigDecimal("149.90"), result.getPrice());
        verify(auditService).record(
            eq(AuditAction.OFFER_UPDATED), eq("OFFER"),
            org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
            eq("Premium"), anyMap(), anyMap(), org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void shouldRejectDuplicateNameOnUpdate() {
        Offer offer = offer(service());
        when(offerRepository.findByPublicIdAndArchived(offerId(), false))
            .thenReturn(Optional.of(offer));
        when(offerRepository.existsByCatalogService_IdAndNameAndIdNot(null, "Premium", null))
            .thenReturn(true);
        SaveOfferRequest request = new SaveOfferRequest(
            "Premium", null, BigDecimal.ZERO, 1, 1,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)
        );
        assertThrows(BusinessRuleException.class, () -> management.updateOffer(offerId(), request));
        verify(offerRepository, never()).save(any());
    }

    @Test
    void shouldArchiveActiveOfferAndAudit() {
        Offer offer = offer(service());
        when(offerRepository.findByPublicIdAndArchived(offerId(), false))
            .thenReturn(Optional.of(offer));
        when(offerRepository.save(offer)).thenReturn(offer);

        Offer result = management.archiveOffer(offerId());

        assertTrue(result.isArchived());
        verify(auditService).record(
            eq(AuditAction.OFFER_ARCHIVED), eq("OFFER"),
            org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
            eq("Standard"), anyMap(), anyMap(), org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void shouldRestoreOfferWhenParentServiceIsActive() {
        Offer offer = offer(service());
        offer.archive();
        when(offerRepository.findByPublicIdAndArchived(offerId(), true))
            .thenReturn(Optional.of(offer));
        when(offerRepository.save(offer)).thenReturn(offer);

        Offer result = management.restoreOffer(offerId());

        assertFalse(result.isArchived());
        verify(auditService).record(
            eq(AuditAction.OFFER_RESTORED), eq("OFFER"),
            org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
            eq("Standard"), anyMap(), anyMap(), org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void shouldRejectOfferRestoreWhileParentServiceIsArchived() {
        CatalogService service = service();
        service.archive();
        Offer offer = offer(service);
        offer.archive();
        when(offerRepository.findByPublicIdAndArchived(offerId(), true))
            .thenReturn(Optional.of(offer));

        assertThrows(BusinessRuleException.class, () -> management.restoreOffer(offerId()));

        verify(offerRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void shouldDeleteUnreferencedArchivedOfferAndAudit() {
        Offer offer = offer(service());
        offer.archive();
        when(offerRepository.findByPublicIdAndArchived(offerId(), true))
            .thenReturn(Optional.of(offer));

        management.deleteArchivedOffer(offerId(), "Offer permanently retired");

        verify(offerRepository).delete(offer);
        verify(auditService).record(
            eq(AuditAction.OFFER_DELETED), eq("OFFER"),
            org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
            eq("Standard"), anyMap(), anyMap(), eq("Offer permanently retired")
        );
    }

    private SaveOfferRequest createRequest() {
        return new SaveOfferRequest(
            " Standard ", " Base plan ", new BigDecimal("99.90"), 12, 10,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );
    }

    private CatalogService service() {
        return new CatalogService("Cloud Backup", null, null);
    }

    private Offer offer(CatalogService service) {
        return new Offer(
            service, "Standard", null, new BigDecimal("99.90"), 12, 10,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );
    }

    private UUID serviceId() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    private UUID offerId() {
        return UUID.fromString("22222222-2222-2222-2222-222222222222");
    }
}
