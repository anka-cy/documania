package com.documania.backend.ticket.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketAttachmentResponse(
    UUID publicId,
    UUID ticketId,
    String fileName,
    String mimeType,
    long fileSize,
    UUID uploadedById,
    String uploadedByEmail,
    LocalDateTime createdAt
) {}
