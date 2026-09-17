package com.documania.backend.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionPeriodResponse(
    int periodNumber, LocalDate startDate, LocalDate endDate, BigDecimal price,
    String offerName, int durationMonths, int numberOfUsers, String serviceName,
    UUID createdById, String createdByEmail, LocalDateTime createdAt
) {}
