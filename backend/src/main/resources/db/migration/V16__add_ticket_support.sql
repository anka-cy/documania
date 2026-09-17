-- Veviosys v2 - Support Ticket System with Deliverables Checklist
-- Target: MySQL 8.4
-- Flyway migration: V16__add_ticket_support.sql

CREATE TABLE `tickets` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `public_id` CHAR(36) NOT NULL,
    `client_id` BIGINT NULL,
    `client_company_name_snapshot` VARCHAR(255) NOT NULL,
    `order_id` BIGINT NULL,
    `subscription_id` BIGINT NULL,
    `assigned_to_user_id` BIGINT NULL,
    `category` VARCHAR(50) NOT NULL,
    `priority` VARCHAR(20) NOT NULL,
    `status` VARCHAR(30) NOT NULL,
    `subject` VARCHAR(255) NOT NULL,
    `description` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `resolved_at` DATETIME NULL,
    `closed_at` DATETIME NULL,
    CONSTRAINT `pk_tickets` PRIMARY KEY (`id`),
    CONSTRAINT `uk_tickets_public_id` UNIQUE (`public_id`),
    CONSTRAINT `fk_tickets_client` FOREIGN KEY (`client_id`) REFERENCES `clients` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_tickets_order` FOREIGN KEY (`order_id`) REFERENCES `customer_orders` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_tickets_subscription` FOREIGN KEY (`subscription_id`) REFERENCES `subscriptions` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_tickets_assigned_user` FOREIGN KEY (`assigned_to_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_tickets_client_id` ON `tickets` (`client_id`);
CREATE INDEX `idx_tickets_order_id` ON `tickets` (`order_id`);
CREATE INDEX `idx_tickets_subscription_id` ON `tickets` (`subscription_id`);
CREATE INDEX `idx_tickets_assigned_to_user_id` ON `tickets` (`assigned_to_user_id`);
CREATE INDEX `idx_tickets_status` ON `tickets` (`status`);
CREATE INDEX `idx_tickets_category` ON `tickets` (`category`);

CREATE TABLE `ticket_messages` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `public_id` CHAR(36) NOT NULL,
    `ticket_id` BIGINT NOT NULL,
    `author_id` BIGINT NOT NULL,
    `message` TEXT NOT NULL,
    `is_internal` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_ticket_messages` PRIMARY KEY (`id`),
    CONSTRAINT `uk_ticket_messages_public_id` UNIQUE (`public_id`),
    CONSTRAINT `fk_ticket_messages_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ticket_messages_author` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_ticket_messages_ticket_id` ON `ticket_messages` (`ticket_id`);

CREATE TABLE `ticket_tasks` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `public_id` CHAR(36) NOT NULL,
    `ticket_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    `position` INT NOT NULL DEFAULT 0,
    `completed_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_ticket_tasks` PRIMARY KEY (`id`),
    CONSTRAINT `uk_ticket_tasks_public_id` UNIQUE (`public_id`),
    CONSTRAINT `fk_ticket_tasks_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_ticket_tasks_ticket_id` ON `ticket_tasks` (`ticket_id`);

-- Add TICKET_* permissions
INSERT INTO `permissions` (`code`, `description`)
SELECT `seed`.`code`, `seed`.`description`
FROM (
    SELECT 'TICKET_READ' AS `code`, 'Consulter les tickets' AS `description`
    UNION ALL SELECT 'TICKET_UPDATE', 'Modifier les tickets'
    UNION ALL SELECT 'TICKET_ASSIGN', 'Assigner des tickets au staff'
    UNION ALL SELECT 'TICKET_DELETE', 'Supprimer des tickets'
) AS `seed`
LEFT JOIN `permissions` AS `existing_permission`
    ON `existing_permission`.`code` = `seed`.`code`
WHERE `existing_permission`.`id` IS NULL;

-- ADMIN gets all TICKET_* permissions
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT `admin_role`.`id`, `permission`.`id`
FROM `roles` AS `admin_role`
CROSS JOIN `permissions` AS `permission`
LEFT JOIN `role_permissions` AS `existing_assignment`
    ON `existing_assignment`.`role_id` = `admin_role`.`id`
   AND `existing_assignment`.`permission_id` = `permission`.`id`
WHERE `admin_role`.`name` = 'ADMIN'
  AND `permission`.`code` IN ('TICKET_READ', 'TICKET_UPDATE', 'TICKET_ASSIGN', 'TICKET_DELETE')
  AND `existing_assignment`.`role_id` IS NULL;

-- STAFF gets TICKET_READ, TICKET_UPDATE, TICKET_ASSIGN
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT `staff_role`.`id`, `permission`.`id`
FROM `roles` AS `staff_role`
CROSS JOIN `permissions` AS `permission`
LEFT JOIN `role_permissions` AS `existing_assignment`
    ON `existing_assignment`.`role_id` = `staff_role`.`id`
   AND `existing_assignment`.`permission_id` = `permission`.`id`
WHERE `staff_role`.`name` = 'STAFF'
  AND `permission`.`code` IN ('TICKET_READ', 'TICKET_UPDATE', 'TICKET_ASSIGN')
  AND `existing_assignment`.`role_id` IS NULL;