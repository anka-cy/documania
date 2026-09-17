package com.documania.backend.catalog;

import com.documania.backend.catalog.dto.CatalogueResponse;
import com.documania.backend.offer.Offer;
import com.documania.backend.offer.OfferRepository;
import com.documania.backend.catalog.dto.CatalogueResponse.CatalogueOfferResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogueQueryService {

    private final CatalogServiceRepository serviceRepository;
    private final OfferRepository offerRepository;

    public CatalogueQueryService(CatalogServiceRepository serviceRepository, OfferRepository offerRepository) {
        this.serviceRepository = serviceRepository;
        this.offerRepository = offerRepository;
    }

    public List<CatalogueResponse> getActiveCatalogue() {
        List<CatalogService> services = serviceRepository.findAllByArchivedOrderByCreatedAtDesc(false);
        List<CatalogueResponse> responses = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (CatalogService service : services) {
            List<CatalogueOfferResponse> offers = new ArrayList<>();
            for (Offer offer : offerRepository.findAllByCatalogService_IdAndArchivedOrderByCreatedAtDesc(
                service.getId(), false)) {
                if (isCommerciallyAvailable(offer, today)) {
                    offers.add(new CatalogueOfferResponse(
                        offer.getPublicId(),
                        offer.getName(),
                        offer.getDescription(),
                        offer.getPrice(),
                        offer.getDurationMonths(),
                        offer.getNumberOfUsers(),
                        offer.getCommercialStartDate(),
                        offer.getCommercialEndDate()
                    ));
                }
            }
            responses.add(new CatalogueResponse(
                service.getPublicId(),
                service.getName(),
                service.getDescription(),
                service.getCategory(),
                offers
            ));
        }
        return responses;
    }

    private boolean isCommerciallyAvailable(Offer offer, LocalDate today) {
        return !today.isBefore(offer.getCommercialStartDate())
            && !today.isAfter(offer.getCommercialEndDate());
    }
}