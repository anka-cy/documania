-- Veviosys v2 - Remove SUBSCRIPTION_RENEW permission
-- Target: MySQL 8.4
-- Flyway migration: V8__remove_subscription_renew_permission.sql

-- The manual renewal feature was removed. Remove the permission and its
-- role links (FK on role_permissions.permission_id).
DELETE rp FROM `role_permissions` rp
    JOIN `permissions` p ON p.`id` = rp.`permission_id`
    WHERE p.`code` = 'SUBSCRIPTION_RENEW';

DELETE FROM `permissions`
    WHERE `code` = 'SUBSCRIPTION_RENEW';
