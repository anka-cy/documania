package com.documania.backend.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketTaskRequest(
    @NotBlank(message = "Le titre de la tâche est obligatoire")
    @Size(max = 255, message = "Le titre ne peut pas dépasser 255 caractères") String title,
    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères") String description
) {}
