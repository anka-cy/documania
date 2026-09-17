package com.documania.backend.offer;

import com.documania.backend.offer.dto.SaveOfferRequest;
import com.documania.backend.offer.dto.OfferResponse;
import com.documania.backend.common.dto.DeleteReasonRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff")
public class OfferController {

    private final OfferManagementService offerManagement;

    public OfferController(OfferManagementService offerManagement) {
        this.offerManagement = offerManagement;
    }

    @PostMapping("/services/{serviceId}/offers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('OFFER_CREATE')")
    public OfferResponse createOffer(
        @PathVariable UUID serviceId,
        @Valid @RequestBody SaveOfferRequest request
    ) {
        return OfferMapper.toResponse(offerManagement.createOffer(serviceId, request));
    }

    @GetMapping("/services/{serviceId}/offers")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('OFFER_READ')")
    public List<OfferResponse> listActiveOffers(@PathVariable UUID serviceId) {
        List<OfferResponse> responses = new ArrayList<>();
        for (Offer offer : offerManagement.listActiveOffers(serviceId)) {
            responses.add(OfferMapper.toResponse(offer));
        }
        return responses;
    }

    @GetMapping("/services/{serviceId}/offers/archived")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ARCHIVE_READ')")
    public List<OfferResponse> listArchivedOffers(@PathVariable UUID serviceId) {
        List<OfferResponse> responses = new ArrayList<>();
        for (Offer offer : offerManagement.listArchivedOffers(serviceId)) {
            responses.add(OfferMapper.toResponse(offer));
        }
        return responses;
    }

    @PutMapping("/offers/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('OFFER_UPDATE')")
    public OfferResponse updateOffer(
        @PathVariable UUID id,
        @Valid @RequestBody SaveOfferRequest request
    ) {
        return OfferMapper.toResponse(offerManagement.updateOffer(id, request));
    }

    @PatchMapping("/offers/{id}/archive")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('OFFER_ARCHIVE')")
    public OfferResponse archiveOffer(@PathVariable UUID id) {
        return OfferMapper.toResponse(offerManagement.archiveOffer(id));
    }

    @PatchMapping("/offers/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('OFFER_RESTORE')")
    public OfferResponse restoreOffer(@PathVariable UUID id) {
        return OfferMapper.toResponse(offerManagement.restoreOffer(id));
    }

    @DeleteMapping("/offers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteArchivedOffer(
        @PathVariable UUID id,
        @Valid @RequestBody DeleteReasonRequest request
    ) {
        offerManagement.deleteArchivedOffer(id, request.reason());
    }
}
