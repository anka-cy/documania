-- Veviosys v2 - Public UUID identifiers for API-facing resources
-- Target: MySQL 8.4
-- Flyway migration: V4__add_public_identifiers.sql

ALTER TABLE `users` ADD COLUMN `public_id` CHAR(36) NULL AFTER `id`;
UPDATE `users` SET `public_id` = UUID() WHERE `public_id` IS NULL;
ALTER TABLE `users`
    MODIFY COLUMN `public_id` CHAR(36) NOT NULL,
    ADD CONSTRAINT `uk_users_public_id` UNIQUE (`public_id`);

ALTER TABLE `clients` ADD COLUMN `public_id` CHAR(36) NULL AFTER `id`;
UPDATE `clients` SET `public_id` = UUID() WHERE `public_id` IS NULL;
ALTER TABLE `clients`
    MODIFY COLUMN `public_id` CHAR(36) NOT NULL,
    ADD CONSTRAINT `uk_clients_public_id` UNIQUE (`public_id`);

ALTER TABLE `services` ADD COLUMN `public_id` CHAR(36) NULL AFTER `id`;
UPDATE `services` SET `public_id` = UUID() WHERE `public_id` IS NULL;
ALTER TABLE `services`
    MODIFY COLUMN `public_id` CHAR(36) NOT NULL,
    ADD CONSTRAINT `uk_services_public_id` UNIQUE (`public_id`);

ALTER TABLE `offers` ADD COLUMN `public_id` CHAR(36) NULL AFTER `id`;
UPDATE `offers` SET `public_id` = UUID() WHERE `public_id` IS NULL;
ALTER TABLE `offers`
    MODIFY COLUMN `public_id` CHAR(36) NOT NULL,
    ADD CONSTRAINT `uk_offers_public_id` UNIQUE (`public_id`);

ALTER TABLE `customer_orders` ADD COLUMN `public_id` CHAR(36) NULL AFTER `id`;
UPDATE `customer_orders` SET `public_id` = UUID() WHERE `public_id` IS NULL;
ALTER TABLE `customer_orders`
    MODIFY COLUMN `public_id` CHAR(36) NOT NULL,
    ADD CONSTRAINT `uk_customer_orders_public_id` UNIQUE (`public_id`);

ALTER TABLE `subscriptions` ADD COLUMN `public_id` CHAR(36) NULL AFTER `id`;
UPDATE `subscriptions` SET `public_id` = UUID() WHERE `public_id` IS NULL;
ALTER TABLE `subscriptions`
    MODIFY COLUMN `public_id` CHAR(36) NOT NULL,
    ADD CONSTRAINT `uk_subscriptions_public_id` UNIQUE (`public_id`);
