package com.documania.backend.ticket.dto;

import com.documania.backend.ticket.TicketTaskStatus;
import jakarta.validation.constraints.Size;

public record UpdateTicketTaskRequest(
    @Size(max = 255, message = "Le titre ne peut pas dépasser 255 caractères") String title,
    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères") String description,
    TicketTaskStatus status
) {}
