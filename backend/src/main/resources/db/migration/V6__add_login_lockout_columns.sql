ALTER TABLE `users`
    ADD COLUMN `failed_login_attempts` INT NOT NULL DEFAULT 0 AFTER `enabled`,
    ADD COLUMN `locked_until` DATETIME NULL AFTER `failed_login_attempts`;