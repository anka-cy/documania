package com.documania.backend.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceResponse(
    String invoiceNumber,
    String orderNumber,
    String clientCompanyName,
    String offerName,
    String serviceName,
    Integer durationMonths,
    BigDecimal price,
    LocalDateTime issuedAt
) {}
