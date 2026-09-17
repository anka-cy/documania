package com.documania.backend.dashboard.dto;

import com.documania.backend.audit.dto.AuditResponse;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
    EntityCounts clients,
    EntityCounts services,
    EntityCounts offers,
    OrderCounts orders,
    SubscriptionCounts subscriptions,
    BigDecimal confirmedRevenue,
    List<AuditResponse> recentAuditEvents
) {
    public record EntityCounts(long total, long active, long archived) {
    }

    public record OrderCounts(
        long total,
        long pending,
        long confirmed
    ) {
    }

    public record SubscriptionCounts(
        long total,
        long active,
        long expiringSoon
    ) {
    }
}