package com.documania.backend.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveCatalogServiceRequest(
    @NotBlank(message = "Le nom du service est obligatoire")
    @Size(max = 100, message = "Le nom du service ne peut pas dépasser 100 caractères")
    String name,

    @Size(max = 5000, message = "La description ne peut pas dépasser 5000 caractères")
    String description,

    @Size(max = 100, message = "La catégorie ne peut pas dépasser 100 caractères")
    String category
) {
}
