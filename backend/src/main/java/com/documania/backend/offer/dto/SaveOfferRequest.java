package com.documania.backend.offer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaveOfferRequest(
    @NotBlank(message = "Le nom de l'offre est obligatoire")
    @Size(max = 100, message = "Le nom de l'offre ne peut pas dépasser 100 caractères")
    String name,

    @Size(max = 5000, message = "La description ne peut pas dépasser 5000 caractères")
    String description,

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.00", message = "Le prix doit être positif ou nul")
    @Digits(integer = 10, fraction = 2, message = "Le prix accepte au maximum 10 chiffres et 2 décimales")
    BigDecimal price,

    @Positive(message = "La durée doit être strictement positive")
    int durationMonths,

    @Positive(message = "Le nombre d'utilisateurs doit être strictement positif")
    int numberOfUsers,

    @NotNull(message = "La date de début commercial est obligatoire")
    LocalDate commercialStartDate,

    @NotNull(message = "La date de fin commerciale est obligatoire")
    LocalDate commercialEndDate
) {
}
