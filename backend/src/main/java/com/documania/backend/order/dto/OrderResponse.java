package com.documania.backend.order.dto;

import com.documania.backend.order.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
    UUID publicId, String orderNumber, UUID clientId, String clientCompanyName,
    UUID offerId, OrderStatus status, BigDecimal price, String offerName,
    int durationMonths, int numberOfUsers, String serviceName,
    String rejectionReason, UUID processedById, String processedByEmail,
    LocalDateTime processedAt, LocalDateTime cancelledAt,
    LocalDateTime createdAt, LocalDateTime updatedAt
) {}
