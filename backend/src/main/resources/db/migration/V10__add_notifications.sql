-- Veviosys v2 - Notifications table
-- Target: MySQL 8.4
-- Flyway migration: V10__add_notifications.sql

CREATE TABLE `notifications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `public_id` CHAR(36) NOT NULL,
    `recipient_id` BIGINT NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `message` VARCHAR(500) NOT NULL,
    `link` VARCHAR(255) NULL,
    `read_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_notifications` PRIMARY KEY (`id`),
    CONSTRAINT `uk_notifications_public_id` UNIQUE (`public_id`),
    CONSTRAINT `fk_notifications_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_notifications_recipient_read` ON `notifications` (`recipient_id`, `read_at`);