-- Veviosys v2 - Initial Database Schema
-- Target: MySQL 8.4
-- Flyway migration: V1__create_initial_schema.sql

SET NAMES utf8mb4;

-- 1. roles
CREATE TABLE `roles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `description` VARCHAR(255) NULL,
    `portal_type` VARCHAR(20) NOT NULL,
    `system_role` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_roles` PRIMARY KEY (`id`),
    CONSTRAINT `uk_roles_name` UNIQUE (`name`),
    CONSTRAINT `ck_roles_portal_type` CHECK (`portal_type` IN ('STAFF', 'CLIENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. permissions
CREATE TABLE `permissions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(100) NOT NULL,
    `description` VARCHAR(255) NULL,
    CONSTRAINT `pk_permissions` PRIMARY KEY (`id`),
    CONSTRAINT `uk_permissions_code` UNIQUE (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. role_permissions
CREATE TABLE `role_permissions` (
    `role_id` BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    CONSTRAINT `pk_role_permissions` PRIMARY KEY (`role_id`, `permission_id`),
    CONSTRAINT `fk_role_permissions_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_role_permissions_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. users
CREATE TABLE `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `role_id` BIGINT NOT NULL,
    `first_name` VARCHAR(100) NOT NULL,
    `last_name` VARCHAR(100) NOT NULL,
    `email_verified` BOOLEAN NOT NULL DEFAULT FALSE,
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_users` PRIMARY KEY (`id`),
    CONSTRAINT `uk_users_email` UNIQUE (`email`),
    CONSTRAINT `fk_users_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. clients
CREATE TABLE `clients` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `company_name` VARCHAR(150) NOT NULL,
    `phone` VARCHAR(30) NULL,
    `address` TEXT NULL,
    `sector` VARCHAR(100) NULL,
    `image_url` VARCHAR(500) NULL,
    `archived` BOOLEAN NOT NULL DEFAULT FALSE,
    `archived_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_clients` PRIMARY KEY (`id`),
    CONSTRAINT `uk_clients_user_id` UNIQUE (`user_id`),
    CONSTRAINT `fk_clients_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. refresh_tokens
CREATE TABLE `refresh_tokens` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `token_hash` VARCHAR(255) NOT NULL,
    `expires_at` DATETIME NOT NULL,
    `revoked_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_refresh_tokens` PRIMARY KEY (`id`),
    CONSTRAINT `uk_refresh_tokens_token_hash` UNIQUE (`token_hash`),
    CONSTRAINT `fk_refresh_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ck_refresh_tokens_expires_after_created` CHECK (`expires_at` > `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. account_tokens
CREATE TABLE `account_tokens` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `token_hash` VARCHAR(255) NOT NULL,
    `type` ENUM('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'PASSWORD_SETUP') NOT NULL,
    `expires_at` DATETIME NOT NULL,
    `used_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_account_tokens` PRIMARY KEY (`id`),
    CONSTRAINT `uk_account_tokens_token_hash` UNIQUE (`token_hash`),
    CONSTRAINT `fk_account_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ck_account_tokens_expires_after_created` CHECK (`expires_at` > `created_at`),
    CONSTRAINT `ck_account_tokens_used_at_null_or_after_created` CHECK (`used_at` IS NULL OR `used_at` >= `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. services
CREATE TABLE `services` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT NULL,
    `category` VARCHAR(100) NULL,
    `image_url` VARCHAR(500) NULL,
    `archived` BOOLEAN NOT NULL DEFAULT FALSE,
    `archived_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_services` PRIMARY KEY (`id`),
    CONSTRAINT `uk_services_name` UNIQUE (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. offers
CREATE TABLE `offers` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `service_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT NULL,
    `price` DECIMAL(12,2) NOT NULL,
    `duration_months` INT NOT NULL,
    `number_of_users` INT NOT NULL,
    `commercial_start_date` DATE NOT NULL,
    `commercial_end_date` DATE NOT NULL,
    `archived` BOOLEAN NOT NULL DEFAULT FALSE,
    `archived_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_offers` PRIMARY KEY (`id`),
    CONSTRAINT `uk_offers_service_id_name` UNIQUE (`service_id`, `name`),
    CONSTRAINT `fk_offers_service` FOREIGN KEY (`service_id`) REFERENCES `services` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `ck_offers_price_nonneg` CHECK (`price` >= 0),
    CONSTRAINT `ck_offers_duration_months_pos` CHECK (`duration_months` > 0),
    CONSTRAINT `ck_offers_number_of_users_pos` CHECK (`number_of_users` > 0),
    CONSTRAINT `ck_offers_commercial_dates_order` CHECK (`commercial_end_date` >= `commercial_start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. customer_orders
CREATE TABLE `customer_orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `client_id` BIGINT NOT NULL,
    `offer_id` BIGINT NOT NULL,
    `order_number` VARCHAR(30) NOT NULL,
    `status` ENUM('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    `rejection_reason` TEXT NULL,
    `price_snapshot` DECIMAL(12,2) NOT NULL,
    `offer_name_snapshot` VARCHAR(100) NOT NULL,
    `offer_duration_months_snapshot` INT NOT NULL,
    `offer_number_of_users_snapshot` INT NOT NULL,
    `service_name_snapshot` VARCHAR(100) NOT NULL,
    `processed_by_user_id` BIGINT NULL,
    `processed_at` DATETIME NULL,
    `cancelled_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_customer_orders` PRIMARY KEY (`id`),
    CONSTRAINT `uk_customer_orders_order_number` UNIQUE (`order_number`),
    CONSTRAINT `fk_customer_orders_client` FOREIGN KEY (`client_id`) REFERENCES `clients` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_customer_orders_offer` FOREIGN KEY (`offer_id`) REFERENCES `offers` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_customer_orders_processed_by` FOREIGN KEY (`processed_by_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_customer_orders_price_snapshot_nonneg` CHECK (`price_snapshot` >= 0),
    CONSTRAINT `ck_customer_orders_duration_snapshot_pos` CHECK (`offer_duration_months_snapshot` > 0),
    CONSTRAINT `ck_customer_orders_number_of_users_snapshot_pos` CHECK (`offer_number_of_users_snapshot` > 0),
    CONSTRAINT `ck_customer_orders_rejection_reason_required` CHECK (
        (`status` <> 'REJECTED') OR (`rejection_reason` IS NOT NULL AND TRIM(`rejection_reason`) <> '')
    ),
    CONSTRAINT `ck_customer_orders_processed_at_consistency` CHECK (
        (`status` NOT IN ('CONFIRMED', 'REJECTED')) OR (`processed_at` IS NOT NULL)
    ),
    CONSTRAINT `ck_customer_orders_cancelled_at_consistency` CHECK (
        (`status` <> 'CANCELLED') OR (`cancelled_at` IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. subscriptions
CREATE TABLE `subscriptions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `client_id` BIGINT NOT NULL,
    `offer_id` BIGINT NOT NULL,
    `customer_order_id` BIGINT NULL,
    `status` ENUM('ACTIVE', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    `cancellation_reason` TEXT NULL,
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `cancelled_at` DATETIME NULL,
    `cancelled_by` BIGINT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_subscriptions` PRIMARY KEY (`id`),
    CONSTRAINT `uk_subscriptions_customer_order_id` UNIQUE (`customer_order_id`),
    CONSTRAINT `fk_subscriptions_client` FOREIGN KEY (`client_id`) REFERENCES `clients` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_subscriptions_offer` FOREIGN KEY (`offer_id`) REFERENCES `offers` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_subscriptions_customer_order` FOREIGN KEY (`customer_order_id`) REFERENCES `customer_orders` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_subscriptions_cancelled_by` FOREIGN KEY (`cancelled_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_subscriptions_dates_order` CHECK (`end_date` >= `start_date`),
    CONSTRAINT `ck_subscriptions_cancellation_reason_required` CHECK (
        (`status` <> 'CANCELLED') OR (`cancellation_reason` IS NOT NULL AND TRIM(`cancellation_reason`) <> '')
    ),
    CONSTRAINT `ck_subscriptions_cancelled_at_consistency` CHECK (
        (`status` <> 'CANCELLED') OR (`cancelled_at` IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. subscription_periods
CREATE TABLE `subscription_periods` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `subscription_id` BIGINT NOT NULL,
    `period_number` INT NOT NULL,
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `price_snapshot` DECIMAL(12,2) NOT NULL,
    `offer_name_snapshot` VARCHAR(100) NOT NULL,
    `offer_duration_months_snapshot` INT NOT NULL,
    `offer_number_of_users_snapshot` INT NOT NULL,
    `service_name_snapshot` VARCHAR(100) NOT NULL,
    `created_by_user_id` BIGINT NULL,
    `renewal_reason` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_subscription_periods` PRIMARY KEY (`id`),
    CONSTRAINT `uk_subscription_periods_subscription_period` UNIQUE (`subscription_id`, `period_number`),
    CONSTRAINT `fk_subscription_periods_subscription` FOREIGN KEY (`subscription_id`) REFERENCES `subscriptions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_subscription_periods_created_by` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_subscription_periods_dates_order` CHECK (`end_date` >= `start_date`),
    CONSTRAINT `ck_subscription_periods_price_snapshot_nonneg` CHECK (`price_snapshot` >= 0),
    CONSTRAINT `ck_subscription_periods_duration_snapshot_pos` CHECK (`offer_duration_months_snapshot` > 0),
    CONSTRAINT `ck_subscription_periods_number_of_users_snapshot_pos` CHECK (`offer_number_of_users_snapshot` > 0),
    CONSTRAINT `ck_subscription_periods_period_number_pos` CHECK (`period_number` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. audit_logs
CREATE TABLE `audit_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `actor_user_id` BIGINT NULL,
    `actor_email` VARCHAR(255) NOT NULL,
    `actor_role` VARCHAR(50) NOT NULL,
    `action` VARCHAR(100) NOT NULL,
    `entity_type` VARCHAR(50) NOT NULL,
    `entity_id` BIGINT NOT NULL,
    `entity_label` VARCHAR(150) NULL,
    `old_values` JSON NULL,
    `new_values` JSON NULL,
    `reason` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_audit_logs` PRIMARY KEY (`id`),
    CONSTRAINT `fk_audit_logs_actor_user` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. email_outbox
CREATE TABLE `email_outbox` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `recipient` VARCHAR(255) NOT NULL,
    `subject` VARCHAR(255) NOT NULL,
    `body` TEXT NOT NULL,
    `html` BOOLEAN NOT NULL DEFAULT FALSE,
    `status` ENUM('PENDING', 'SENDING', 'SENT', 'FAILED') NOT NULL DEFAULT 'PENDING',
    `attempts` INT NOT NULL DEFAULT 0,
    `max_attempts` INT NOT NULL DEFAULT 5,
    `last_error` TEXT NULL,
    `next_retry_at` DATETIME NULL,
    `sent_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_email_outbox` PRIMARY KEY (`id`),
    CONSTRAINT `ck_email_outbox_attempts_nonneg` CHECK (`attempts` >= 0),
    CONSTRAINT `ck_email_outbox_max_attempts_pos` CHECK (`max_attempts` > 0),
    CONSTRAINT `ck_email_outbox_attempts_not_exceed_max` CHECK (`attempts` <= `max_attempts`),
    CONSTRAINT `ck_email_outbox_sent_at_consistency` CHECK (
        (`status` <> 'SENT') OR (`sent_at` IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
