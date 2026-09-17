-- Veviosys v2 - Unread tracking for ticket discussions
-- Target: MySQL 8.4
-- Flyway migration: V18__add_ticket_read_marks.sql

CREATE TABLE `ticket_read_marks` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `ticket_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `last_read_at` DATETIME NOT NULL,
    CONSTRAINT `pk_ticket_read_marks` PRIMARY KEY (`id`),
    CONSTRAINT `uk_ticket_read_marks_ticket_user` UNIQUE (`ticket_id`, `user_id`),
    CONSTRAINT `fk_ticket_read_marks_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ticket_read_marks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_ticket_read_marks_user` ON `ticket_read_marks` (`user_id`);