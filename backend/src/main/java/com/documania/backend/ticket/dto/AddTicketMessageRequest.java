package com.documania.backend.ticket.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AddTicketMessageRequest(
    @Size(max = 5000, message = "Le message ne peut pas dépasser 5000 caractères") String message,
    List<UUID> attachmentIds
) {
}
