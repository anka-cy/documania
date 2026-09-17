-- Veviosys v2 - Remove TICKET_ASSIGN permission (assign feature removed)
-- Target: MySQL 8.4
-- Flyway migration: V17__remove_ticket_assign_permission.sql

DELETE FROM `role_permissions`
WHERE `permission_id` IN (
    SELECT `id` FROM `permissions` WHERE `code` = 'TICKET_ASSIGN'
);

DELETE FROM `permissions` WHERE `code` = 'TICKET_ASSIGN';