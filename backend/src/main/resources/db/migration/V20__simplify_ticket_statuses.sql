-- Veviosys v2 - Simplify ticket statuses (remove WAITING_CLIENT, RESOLVED)
-- Target: MySQL 8.4
-- Flyway migration: V20__simplify_ticket_statuses.sql

-- WAITING_CLIENT → IN_PROGRESS (le staff attendait le client ; le ticket reste en cours)
UPDATE `tickets` SET `status` = 'IN_PROGRESS' WHERE `status` = 'WAITING_CLIENT';

-- RESOLVED → CLOSED (les tickets résolus sont désormais clôturés)
UPDATE `tickets` SET `status` = 'CLOSED', `closed_at` = COALESCE(`closed_at`, `updated_at`)
WHERE `status` = 'RESOLVED';
