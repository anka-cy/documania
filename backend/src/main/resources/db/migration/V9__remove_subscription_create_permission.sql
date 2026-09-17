-- Veviosys v2 - Remove SUBSCRIPTION_CREATE permission
-- Target: MySQL 8.4
-- Flyway migration: V9__remove_subscription_create_permission.sql

-- Manual subscription creation was removed in favor of staff order creation.
-- Every subscription now comes from a confirmed order.
DELETE rp FROM `role_permissions` rp
    JOIN `permissions` p ON p.`id` = rp.`permission_id`
    WHERE p.`code` = 'SUBSCRIPTION_CREATE';

DELETE FROM `permissions`
    WHERE `code` = 'SUBSCRIPTION_CREATE';