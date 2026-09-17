package com.documania.backend.catalog;

import com.documania.backend.catalog.dto.CatalogServiceResponse;
import com.documania.backend.catalog.dto.SaveCatalogServiceRequest;
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
@RequestMapping("/api/staff/services")
public class CatalogServiceController {

    private final CatalogServiceManagementService serviceManagement;

    public CatalogServiceController(CatalogServiceManagementService serviceManagement) {
        this.serviceManagement = serviceManagement;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SERVICE_CREATE')")
    public CatalogServiceResponse createService(
        @Valid @RequestBody SaveCatalogServiceRequest request
    ) {
        return CatalogServiceMapper.toResponse(serviceManagement.createService(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SERVICE_READ')")
    public List<CatalogServiceResponse> listActiveServices() {
        List<CatalogServiceResponse> responses = new ArrayList<>();
        for (CatalogService service : serviceManagement.listActiveServices()) {
            responses.add(CatalogServiceMapper.toResponse(service));
        }
        return responses;
    }

    @GetMapping("/archived")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ARCHIVE_READ')")
    public List<CatalogServiceResponse> listArchivedServices() {
        List<CatalogServiceResponse> responses = new ArrayList<>();
        for (CatalogService service : serviceManagement.listArchivedServices()) {
            responses.add(CatalogServiceMapper.toResponse(service));
        }
        return responses;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SERVICE_UPDATE')")
    public CatalogServiceResponse updateService(
        @PathVariable UUID id,
        @Valid @RequestBody SaveCatalogServiceRequest request
    ) {
        return CatalogServiceMapper.toResponse(serviceManagement.updateService(id, request));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SERVICE_ARCHIVE')")
    public CatalogServiceResponse archiveService(@PathVariable UUID id) {
        return CatalogServiceMapper.toResponse(serviceManagement.archiveService(id));
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SERVICE_RESTORE')")
    public CatalogServiceResponse restoreService(@PathVariable UUID id) {
        return CatalogServiceMapper.toResponse(serviceManagement.restoreService(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteArchivedService(
        @PathVariable UUID id,
        @Valid @RequestBody DeleteReasonRequest request
    ) {
        serviceManagement.deleteArchivedService(id, request.reason());
    }
}
