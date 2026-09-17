package com.documania.backend.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectOrderRequest(
    @NotBlank(message = "Le motif du rejet est obligatoire")
    @Size(min = 5, max = 500, message = "Le motif doit contenir entre 5 et 500 caractères")
    String reason
) {}
