package com.documania.backend.ticket.dto;

import com.documania.backend.ticket.TicketTaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketTaskResponse(
    UUID publicId,
    String title,
    String description,
    TicketTaskStatus status,
    int position,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
