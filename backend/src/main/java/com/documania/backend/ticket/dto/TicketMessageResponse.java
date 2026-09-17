package com.documania.backend.ticket.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TicketMessageResponse(
    UUID publicId,
    UUID authorId,
    String authorEmail,
    String authorName,
    String authorRole,
    String message,
    boolean internal,
    LocalDateTime createdAt,
    List<TicketAttachmentResponse> attachments
) {}
