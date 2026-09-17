package com.documania.backend.catalog;

import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.catalog.dto.SaveCatalogServiceRequest;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.offer.Offer;
import com.documania.backend.offer.OfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CatalogServiceManagementServiceTest {

    private CatalogServiceRepository repository;
    private AuditService auditService;
    private OfferRepository offerRepository;
    private CatalogServiceManagementService serviceManagement;

    @BeforeEach
    void setUp() {
        repository = mock(CatalogServiceRepository.class);
        auditService = mock(AuditService.class);
        offerRepository = mock(OfferRepository.class);
        serviceManagement = new CatalogServiceManagementService(
            repository, auditService, offerRepository
        );
    }

    @Test
    void shouldCreateNormalizedServiceAndAudit() {
        SaveCatalogServiceRequest request = new SaveCatalogServiceRequest(
            " Cloud Backup ", " Secure backups ", " Cloud "
        );
        when(repository.save(org.mockito.ArgumentMatchers.any(CatalogService.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CatalogService result = serviceManagement.createService(request);

        assertEquals("Cloud Backup", result.getName());
        assertEquals("Secure backups", result.getDescription());
        assertEquals("Cloud", result.getCategory());
        verify(auditService).record(
            eq(AuditAction.SERVICE_CREATED), eq("SERVICE"),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            eq("Cloud Backup"), anyMap(), anyMap(),
            org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void shouldRejectDuplicateNameOnCreate() {
        SaveCatalogServiceRequest request = new SaveCatalogServiceRequest(
            "Cloud Backup", null, null
        );
        when(repository.existsByName("Cloud Backup")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () ->
            serviceManagement.createService(request)
        );

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(auditService);
    }

    @Test
    void shouldListOnlyActiveServices() {
        List<CatalogService> expected = List.of(service());
        when(repository.findAllByArchivedOrderByCreatedAtDesc(false)).thenReturn(expected);

        assertSame(expected, serviceManagement.listActiveServices());
    }

    @Test
    void shouldFindActiveServiceByPublicId() {
        UUID publicId = publicId();
        CatalogService service = service();
        when(repository.findByPublicIdAndArchived(publicId, false))
            .thenReturn(Optional.of(service));

        assertSame(service, serviceManagement.findActiveService(publicId));
    }

    @Test
    void shouldRejectMissingOrArchivedServiceFromActiveRead() {
        UUID publicId = publicId();
        when(repository.findByPublicIdAndArchived(publicId, false)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            serviceManagement.findActiveService(publicId)
        );
    }

    @Test
    void shouldUpdateNormalizedDetailsAndAudit() {
        UUID publicId = publicId();
        CatalogService service = service();
        SaveCatalogServiceRequest request = new SaveCatalogServiceRequest(
            " Cloud Backup Plus ", " Better backups ", " Cloud "
        );
        when(repository.findByPublicIdAndArchived(publicId, false))
            .thenReturn(Optional.of(service));
        when(repository.save(service)).thenReturn(service);

        CatalogService result = serviceManagement.updateService(publicId, request);

        assertEquals("Cloud Backup Plus", result.getName());
        assertEquals("Better backups", result.getDescription());
        verify(auditService).record(
            eq(AuditAction.SERVICE_UPDATED), eq("SERVICE"),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            eq("Cloud Backup Plus"), anyMap(), anyMap(),
            org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void shouldRejectDuplicateNameOnUpdate() {
        UUID publicId = publicId();
        CatalogService service = service();
        SaveCatalogServiceRequest request = new SaveCatalogServiceRequest(
            "Existing Service", null, null
        );
        when(repository.findByPublicIdAndArchived(publicId, false))
            .thenReturn(Optional.of(service));
        when(repository.existsByNameAndIdNot("Existing Service", null)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () ->
            serviceManagement.updateService(publicId, request)
        );

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(auditService);
    }

    @Test
    void shouldArchiveServiceAndAllActiveOffers() {
        UUID publicId = publicId();
        CatalogService service = service();
        Offer first = offer(service, "First");
        Offer second = offer(service, "Second");
        when(repository.findByPublicIdAndArchived(publicId, false))
            .thenReturn(Optional.of(service));
        when(offerRepository.findAllByCatalogService_IdAndArchivedOrderByCreatedAtDesc(null, false))
            .thenReturn(List.of(first, second));
        when(repository.save(service)).thenReturn(service);

        CatalogService result = serviceManagement.archiveService(publicId);

        assertTrue(result.isArchived());
        assertTrue(first.isArchived());
        assertTrue(second.isArchived());
        verify(offerRepository).saveAll(List.of(first, second));
        verify(auditService).record(
            eq(AuditAction.SERVICE_ARCHIVED), eq("SERVICE"),
            org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
            eq("Cloud Backup"), anyMap(), anyMap(), org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void shouldRestoreServiceOnlyAndLeaveOffersArchived() {
        UUID publicId = publicId();
        CatalogService service = service();
        service.archive();
        Offer first = offer(service, "First");
        Offer second = offer(service, "Second");
        first.archive();
        second.archive();
        when(repository.findByPublicIdAndArchived(publicId, true))
            .thenReturn(Optional.of(service));
        when(repository.save(service)).thenReturn(service);

        CatalogService result = serviceManagement.restoreService(publicId);

        assertFalse(result.isArchived());
        assertTrue(first.isArchived());
        assertTrue(second.isArchived());
        verifyNoInteractions(offerRepository);
        verify(auditService).record(
            eq(AuditAction.SERVICE_RESTORED), eq("SERVICE"),
            org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
            eq("Cloud Backup"), anyMap(), anyMap(), org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void shouldDeleteEmptyArchivedServiceAndAudit() {
        UUID publicId = publicId();
        CatalogService service = service();
        service.archive();
        when(repository.findByPublicIdAndArchived(publicId, true))
            .thenReturn(Optional.of(service));

        serviceManagement.deleteArchivedService(publicId, "Service permanently retired");

        verify(repository).delete(service);
        verify(auditService).record(
            eq(AuditAction.SERVICE_DELETED), eq("SERVICE"),
            org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
            eq("Cloud Backup"), anyMap(), anyMap(), eq("Service permanently retired")
        );
    }

    private CatalogService service() {
        return new CatalogService("Cloud Backup", "Secure backups", "Cloud");
    }

    private Offer offer(CatalogService service, String name) {
        return new Offer(
            service, name, null, BigDecimal.TEN, 1, 1,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );
    }

    private UUID publicId() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }
}
