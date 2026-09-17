package com.documania.backend.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteTicketRequest(
    @NotBlank(message = "Le motif de suppression est obligatoire")
    @Size(min = 5, max = 500, message = "Le motif doit contenir entre 5 et 500 caractères")
    String reason
) {}