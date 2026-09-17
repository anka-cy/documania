-- Veviosys v2 - Consistency cleanup
-- Target: MySQL 8.4
-- Flyway migration: V15__drop_service_image_url_and_sending_status.sql
--
-- 1. Drop services.image_url (orphan column): the CatalogService entity, its DTOs
--    and the frontend no longer use it (V14 dropped clients.image_url only).
-- 2. Align email_outbox.status enum with the Java enum EmailOutboxStatus
--    (PENDING, SENT, FAILED): the SENDING value was removed from the enum
--    (D-025) but the database definition still declared it.

ALTER TABLE `services` DROP COLUMN `image_url`;

ALTER TABLE `email_outbox`
    MODIFY COLUMN `status` ENUM('PENDING', 'SENT', 'FAILED') NOT NULL DEFAULT 'PENDING';
