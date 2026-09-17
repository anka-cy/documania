package com.documania.backend.notification;

/** Types de notifications in-app. */
public enum NotificationType {
    ORDER_CONFIRMED,
    ORDER_REJECTED,
    SUBSCRIPTION_EXPIRED,
    SUBSCRIPTION_EXPIRING,
    PASSWORD_CHANGED,
    NEW_ORDER_PENDING,
    TICKET_OPENED,
    TICKET_MESSAGE,
    TICKET_CLOSED,
    TICKET_TASK_COMPLETED
}
