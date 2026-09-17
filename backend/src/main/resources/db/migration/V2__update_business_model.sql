-- Veviosys v2 - Approved business model update
-- Target: MySQL 8.4
-- Flyway migration: V2__update_business_model.sql

-- Roles are fixed to ADMIN, STAFF, and CLIENT.
ALTER TABLE `roles`
    DROP CHECK `ck_roles_portal_type`;

ALTER TABLE `roles`
    DROP COLUMN `portal_type`,
    DROP COLUMN `system_role`;

ALTER TABLE `roles`
    ADD CONSTRAINT `ck_roles_fixed_name`
        CHECK (`name` IN ('ADMIN', 'STAFF', 'CLIENT'));

INSERT INTO `roles` (`name`, `description`)
SELECT 'ADMIN', 'Administrateur avec toutes les permissions'
WHERE NOT EXISTS (
    SELECT 1 FROM `roles` WHERE `name` = 'ADMIN'
);

INSERT INTO `roles` (`name`, `description`)
SELECT 'STAFF', 'Personnel avec des permissions configurables'
WHERE NOT EXISTS (
    SELECT 1 FROM `roles` WHERE `name` = 'STAFF'
);

INSERT INTO `roles` (`name`, `description`)
SELECT 'CLIENT', 'Client limité à son portail et à ses propres ressources'
WHERE NOT EXISTS (
    SELECT 1 FROM `roles` WHERE `name` = 'CLIENT'
);

-- Orders remain after a client is permanently deleted.
-- The company-name snapshot preserves understandable order history.
ALTER TABLE `customer_orders`
    ADD COLUMN `client_company_name_snapshot` VARCHAR(150) NULL AFTER `order_number`;

UPDATE `customer_orders` AS `customer_order`
INNER JOIN `clients` AS `client`
    ON `client`.`id` = `customer_order`.`client_id`
SET `customer_order`.`client_company_name_snapshot` = `client`.`company_name`
WHERE `customer_order`.`client_company_name_snapshot` IS NULL;

ALTER TABLE `customer_orders`
    MODIFY COLUMN `client_company_name_snapshot` VARCHAR(150) NOT NULL;

ALTER TABLE `customer_orders`
    DROP FOREIGN KEY `fk_customer_orders_client`;

ALTER TABLE `customer_orders`
    MODIFY COLUMN `client_id` BIGINT NULL;

ALTER TABLE `customer_orders`
    ADD CONSTRAINT `fk_customer_orders_client`
        FOREIGN KEY (`client_id`) REFERENCES `clients` (`id`) ON DELETE SET NULL;
