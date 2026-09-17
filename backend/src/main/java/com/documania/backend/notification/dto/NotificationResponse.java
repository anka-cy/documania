package com.documania.backend.notification.dto;

import com.documania.backend.notification.Notification;
import com.documania.backend.notification.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
    UUID publicId,
    NotificationType type,
    String title,
    String message,
    String link,
    boolean read,
    LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
            notification.getPublicId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getLink(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }
}
