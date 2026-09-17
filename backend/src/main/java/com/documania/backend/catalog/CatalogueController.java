package com.documania.backend.catalog;

import com.documania.backend.catalog.dto.CatalogueResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/client/catalogue")
public class CatalogueController {

    private final CatalogueQueryService catalogueQueryService;

    public CatalogueController(CatalogueQueryService catalogueQueryService) {
        this.catalogueQueryService = catalogueQueryService;
    }

    @GetMapping
    public List<CatalogueResponse> listActiveCatalogue() {
        return catalogueQueryService.getActiveCatalogue();
    }
}