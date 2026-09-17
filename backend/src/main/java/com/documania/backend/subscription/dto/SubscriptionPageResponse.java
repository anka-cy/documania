package com.documania.backend.subscription.dto;

import java.util.List;

public record SubscriptionPageResponse(
    List<SubscriptionResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
