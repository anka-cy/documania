package com.documania.backend.notification.dto;

import java.util.List;

public record NotificationPageResponse(
    List<NotificationResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
