package com.documania.backend.catalog;

import com.documania.backend.common.util.TextUtils;
import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.catalog.dto.SaveCatalogServiceRequest;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.offer.Offer;
import com.documania.backend.offer.OfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CatalogServiceManagementService {

    private final CatalogServiceRepository catalogServiceRepository;
    private final AuditService auditService;
    private final OfferRepository offerRepository;

    public CatalogServiceManagementService(
        CatalogServiceRepository catalogServiceRepository,
        AuditService auditService,
        OfferRepository offerRepository
    ) {
        this.catalogServiceRepository = catalogServiceRepository;
        this.auditService = auditService;
        this.offerRepository = offerRepository;
    }

    @Transactional
    public CatalogService createService(SaveCatalogServiceRequest request) {
        String name = request.name().trim();
        if (catalogServiceRepository.existsByName(name)) {
            throw new BusinessRuleException("Un service utilise déjà ce nom");
        }

        CatalogService service = new CatalogService(
            name,
            TextUtils.optionalText(request.description()),
            TextUtils.optionalText(request.category())
        );
        CatalogService savedService = catalogServiceRepository.save(service);

        auditService.record(
            AuditAction.SERVICE_CREATED,
            "SERVICE",
            savedService.getId(),
            savedService.getPublicId(),
            savedService.getName(),
            Map.of(),
            serviceSnapshot(savedService),
            null
        );
        return savedService;
    }

    public List<CatalogService> listActiveServices() {
        return catalogServiceRepository.findAllByArchivedOrderByCreatedAtDesc(false);
    }

    public List<CatalogService> listArchivedServices() {
        return catalogServiceRepository.findAllByArchivedOrderByCreatedAtDesc(true);
    }

    public CatalogService findActiveService(UUID publicId) {
        return catalogServiceRepository.findByPublicIdAndArchived(publicId, false)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Service actif introuvable : " + publicId
            ));
    }

    @Transactional
    public CatalogService updateService(UUID publicId, SaveCatalogServiceRequest request) {
        CatalogService service = findActiveService(publicId);
        String name = request.name().trim();
        if (catalogServiceRepository.existsByNameAndIdNot(name, service.getId())) {
            throw new BusinessRuleException("Un service utilise déjà ce nom");
        }

        Map<String, Object> oldValues = serviceSnapshot(service);
        service.changeDetails(
            name,
            TextUtils.optionalText(request.description()),
            TextUtils.optionalText(request.category())
        );
        CatalogService savedService = catalogServiceRepository.save(service);

        auditService.record(
            AuditAction.SERVICE_UPDATED,
            "SERVICE",
            savedService.getId(),
            savedService.getPublicId(),
            savedService.getName(),
            oldValues,
            serviceSnapshot(savedService),
            null
        );
        return savedService;
    }

    @Transactional
    public CatalogService archiveService(UUID publicId) {
        CatalogService service = findActiveService(publicId);
        Map<String, Object> oldValues = serviceSnapshot(service);
        List<Offer> activeOffers =
            offerRepository.findAllByCatalogService_IdAndArchivedOrderByCreatedAtDesc(
                service.getId(), false
            );
        for (Offer offer : activeOffers) {
            offer.archive();
        }
        offerRepository.saveAll(activeOffers);
        service.archive();
        CatalogService savedService = catalogServiceRepository.save(service);

        Map<String, Object> newValues = serviceSnapshot(savedService);
        newValues.put("archivedOfferCount", activeOffers.size());
        auditService.record(
            AuditAction.SERVICE_ARCHIVED, "SERVICE",
            savedService.getId(), savedService.getPublicId(), savedService.getName(),
            oldValues, newValues, null
        );
        return savedService;
    }

    @Transactional
    public CatalogService restoreService(UUID publicId) {
        CatalogService service = catalogServiceRepository.findByPublicIdAndArchived(publicId, true)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Service archivé introuvable : " + publicId
            ));
        Map<String, Object> oldValues = serviceSnapshot(service);
        service.restore();
        CatalogService savedService = catalogServiceRepository.save(service);

        Map<String, Object> newValues = serviceSnapshot(savedService);
        auditService.record(
            AuditAction.SERVICE_RESTORED, "SERVICE",
            savedService.getId(), savedService.getPublicId(), savedService.getName(),
            oldValues, newValues, null
        );
        return savedService;
    }

    @Transactional
    public void deleteArchivedService(UUID publicId, String reason) {
        CatalogService service = catalogServiceRepository.findByPublicIdAndArchived(publicId, true)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Service archivé introuvable : " + publicId
            ));

        // Les offres du service sont supprimées par la base (ON DELETE CASCADE).
        // Les commandes/abonnements qui référençaient ces offres passent en
        // historique via SET NULL + snapshots (nom, prix, durée…).

        auditService.record(
            AuditAction.SERVICE_DELETED, "SERVICE",
            service.getId(), service.getPublicId(), service.getName(),
            serviceSnapshot(service), Map.of(), reason.trim()
        );
        catalogServiceRepository.delete(service);
    }

    private Map<String, Object> serviceSnapshot(CatalogService service) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", service.getName());
        values.put("description", service.getDescription());
        values.put("category", service.getCategory());
        values.put("archived", service.isArchived());
        return values;
    }
}
