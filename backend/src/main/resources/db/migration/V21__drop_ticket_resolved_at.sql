-- Veviosys v2 - Drop dead tickets.resolved_at column
-- Target: MySQL 8.4
-- Flyway migration: V21__drop_ticket_resolved_at.sql

-- La colonne resolved_at n'est plus mappée par l'entité Ticket depuis la
-- simplification des statuts (V20) : le statut RESOLVED n'existe plus.
ALTER TABLE `tickets` DROP COLUMN `resolved_at`;
