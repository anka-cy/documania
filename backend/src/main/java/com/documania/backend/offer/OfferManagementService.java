package com.documania.backend.offer;

import com.documania.backend.common.util.TextUtils;
import com.documania.backend.audit.AuditAction;
import com.documania.backend.audit.AuditService;
import com.documania.backend.catalog.CatalogService;
import com.documania.backend.catalog.CatalogServiceRepository;
import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.offer.dto.SaveOfferRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OfferManagementService {

    private final OfferRepository offerRepository;
    private final CatalogServiceRepository catalogServiceRepository;
    private final AuditService auditService;

    public OfferManagementService(
        OfferRepository offerRepository,
        CatalogServiceRepository catalogServiceRepository,
        AuditService auditService
    ) {
        this.offerRepository = offerRepository;
        this.catalogServiceRepository = catalogServiceRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Offer createOffer(UUID servicePublicId, SaveOfferRequest request) {
        CatalogService catalogService = findActiveCatalogService(servicePublicId);
        validateCommercialDates(request.commercialStartDate(), request.commercialEndDate());

        String name = request.name().trim();
        if (offerRepository.existsByCatalogService_IdAndName(catalogService.getId(), name)) {
            throw new BusinessRuleException("Une offre de ce service utilise déjà ce nom");
        }

        Offer offer = new Offer(
            catalogService,
            name,
            TextUtils.optionalText(request.description()),
            request.price(),
            request.durationMonths(),
            request.numberOfUsers(),
            request.commercialStartDate(),
            request.commercialEndDate()
        );
        Offer savedOffer = offerRepository.save(offer);
        auditService.record(
            AuditAction.OFFER_CREATED,
            "OFFER",
            savedOffer.getId(),
            savedOffer.getPublicId(),
            savedOffer.getName(),
            Map.of(),
            offerSnapshot(savedOffer),
            null
        );
        return savedOffer;
    }

    public List<Offer> listActiveOffers(UUID servicePublicId) {
        CatalogService catalogService = findActiveCatalogService(servicePublicId);
        return offerRepository.findAllByCatalogService_IdAndArchivedOrderByCreatedAtDesc(
            catalogService.getId(),
            false
        );
    }

    public List<Offer> listArchivedOffers(UUID servicePublicId) {
        CatalogService catalogService = findActiveCatalogService(servicePublicId);
        return offerRepository.findAllByCatalogService_IdAndArchivedOrderByCreatedAtDesc(
            catalogService.getId(),
            true
        );
    }

    public Offer findActiveOffer(UUID publicId) {
        Offer offer = offerRepository.findByPublicIdAndArchived(publicId, false)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Offre active introuvable : " + publicId
            ));
        if (offer.getCatalogService().isArchived()) {
            throw new ResourceNotFoundException("Offre active introuvable : " + publicId);
        }
        return offer;
    }

    @Transactional
    public Offer updateOffer(UUID publicId, SaveOfferRequest request) {
        Offer offer = findActiveOffer(publicId);
        validateCommercialDates(request.commercialStartDate(), request.commercialEndDate());

        String name = request.name().trim();
        if (offerRepository.existsByCatalogService_IdAndNameAndIdNot(
            offer.getCatalogService().getId(), name, offer.getId()
        )) {
            throw new BusinessRuleException("Une offre de ce service utilise déjà ce nom");
        }

        Map<String, Object> oldValues = offerSnapshot(offer);
        offer.changeDetails(
            name,
            TextUtils.optionalText(request.description()),
            request.price(),
            request.durationMonths(),
            request.numberOfUsers(),
            request.commercialStartDate(),
            request.commercialEndDate()
        );
        Offer savedOffer = offerRepository.save(offer);
        auditService.record(
            AuditAction.OFFER_UPDATED,
            "OFFER",
            savedOffer.getId(),
            savedOffer.getPublicId(),
            savedOffer.getName(),
            oldValues,
            offerSnapshot(savedOffer),
            null
        );
        return savedOffer;
    }

    @Transactional
    public Offer archiveOffer(UUID publicId) {
        Offer offer = findActiveOffer(publicId);
        Map<String, Object> oldValues = offerSnapshot(offer);
        offer.archive();
        Offer savedOffer = offerRepository.save(offer);
        auditService.record(
            AuditAction.OFFER_ARCHIVED, "OFFER",
            savedOffer.getId(), savedOffer.getPublicId(), savedOffer.getName(),
            oldValues, offerSnapshot(savedOffer), null
        );
        return savedOffer;
    }

    @Transactional
    public Offer restoreOffer(UUID publicId) {
        Offer offer = offerRepository.findByPublicIdAndArchived(publicId, true)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Offre archivée introuvable : " + publicId
            ));
        if (offer.getCatalogService().isArchived()) {
            throw new BusinessRuleException(
                "Une offre ne peut pas être restaurée tant que son service est archivé"
            );
        }
        Map<String, Object> oldValues = offerSnapshot(offer);
        offer.restore();
        Offer savedOffer = offerRepository.save(offer);
        auditService.record(
            AuditAction.OFFER_RESTORED, "OFFER",
            savedOffer.getId(), savedOffer.getPublicId(), savedOffer.getName(),
            oldValues, offerSnapshot(savedOffer), null
        );
        return savedOffer;
    }

    @Transactional
    public void deleteArchivedOffer(UUID publicId, String reason) {
        Offer offer = offerRepository.findByPublicIdAndArchived(publicId, true)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Offre archivée introuvable : " + publicId
            ));

        // Les commandes/abonnements qui référencent cette offre passent en
        // historique via SET NULL + snapshots (nom, prix, durée…).

        auditService.record(
            AuditAction.OFFER_DELETED, "OFFER",
            offer.getId(), offer.getPublicId(), offer.getName(),
            offerSnapshot(offer), Map.of(), reason.trim()
        );
        offerRepository.delete(offer);
    }

    private CatalogService findActiveCatalogService(UUID publicId) {
        return catalogServiceRepository.findByPublicIdAndArchived(publicId, false)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Service actif introuvable : " + publicId
            ));
    }

    private void validateCommercialDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException(
                "La date de fin commerciale ne peut pas précéder la date de début"
            );
        }
    }

    private Map<String, Object> offerSnapshot(Offer offer) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("servicePublicId", offer.getCatalogService().getPublicId());
        values.put("serviceName", offer.getCatalogService().getName());
        values.put("name", offer.getName());
        values.put("description", offer.getDescription());
        values.put("price", offer.getPrice());
        values.put("durationMonths", offer.getDurationMonths());
        values.put("numberOfUsers", offer.getNumberOfUsers());
        values.put("commercialStartDate", offer.getCommercialStartDate());
        values.put("commercialEndDate", offer.getCommercialEndDate());
        values.put("archived", offer.isArchived());
        return values;
    }
}
