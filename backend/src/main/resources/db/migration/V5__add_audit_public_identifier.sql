-- Preserve the public API identifier after an audited entity is changed or deleted.
ALTER TABLE `audit_logs`
    ADD COLUMN `entity_public_id` CHAR(36) NULL AFTER `entity_id`;
