-- Veviosys v2 - Ticket attachments (Azure Blob Storage)
-- Target: MySQL 8.4
-- Flyway migration: V19__add_ticket_attachments.sql

CREATE TABLE `ticket_attachments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `public_id` CHAR(36) NOT NULL,
    `ticket_id` BIGINT NOT NULL,
    `message_id` BIGINT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `blob_name` VARCHAR(500) NOT NULL,
    `mime_type` VARCHAR(100) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `uploaded_by` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_ticket_attachments` PRIMARY KEY (`id`),
    CONSTRAINT `uk_ticket_attachments_public_id` UNIQUE (`public_id`),
    CONSTRAINT `fk_ticket_attachments_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ticket_attachments_message` FOREIGN KEY (`message_id`) REFERENCES `ticket_messages` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_ticket_attachments_uploader` FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_ticket_attachments_ticket` ON `ticket_attachments` (`ticket_id`);
CREATE INDEX `idx_ticket_attachments_message` ON `ticket_attachments` (`message_id`);