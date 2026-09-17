package com.documania.backend.ticket.dto;

import com.documania.backend.ticket.TicketCategory;
import com.documania.backend.ticket.TicketPriority;
import com.documania.backend.ticket.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
    UUID publicId,
    UUID clientId,
    String clientCompanyName,
    UUID orderId,
    String orderNumber,
    UUID subscriptionId,
    String subscriptionServiceName,
    UUID assignedToUserId,
    String assignedToEmail,
    String assignedToName,
    TicketCategory category,
    TicketPriority priority,
    TicketStatus status,
    String subject,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime closedAt,
    int unreadCount
) {}
