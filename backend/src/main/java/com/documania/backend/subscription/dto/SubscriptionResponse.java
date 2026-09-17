package com.documania.backend.subscription.dto;

import com.documania.backend.subscription.SubscriptionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public record SubscriptionResponse(
    UUID publicId, UUID clientId, String clientCompanyName, UUID offerId, String offerName,
    String serviceName, UUID orderId,
    SubscriptionStatus status, LocalDate startDate, LocalDate endDate,
    String cancellationReason, LocalDateTime cancelledAt, UUID cancelledById,
    List<SubscriptionPeriodResponse> periods, LocalDateTime createdAt, LocalDateTime updatedAt
) {}
