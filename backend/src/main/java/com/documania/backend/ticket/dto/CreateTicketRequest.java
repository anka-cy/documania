package com.documania.backend.ticket.dto;

import com.documania.backend.ticket.TicketCategory;
import com.documania.backend.ticket.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTicketRequest(
    UUID subscriptionId,
    @NotNull(message = "La catégorie est obligatoire") TicketCategory category,
    @NotNull(message = "La priorité est obligatoire") TicketPriority priority,
    @NotBlank(message = "L'objet est obligatoire")
    @Size(max = 255, message = "L'objet ne peut pas dépasser 255 caractères") String subject,
    @NotBlank(message = "La description est obligatoire")
    @Size(min = 5, max = 5000, message = "La description doit contenir entre 5 et 5000 caractères") String description
) {}
