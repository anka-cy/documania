package com.documania.backend.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StaffCreateOrderRequest(
    @NotNull(message = "Le client est obligatoire") UUID clientId,
    @NotNull(message = "L'offre est obligatoire") UUID offerId
) {}
