package com.documania.backend.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOrderRequest(@NotNull(message = "L'offre est obligatoire") UUID offerId) {}
