-- Veviosys v2 - Remap orphan audit actions (enum values removed)
-- Target: MySQL 8.4
-- Flyway migration: V23__remap_orphan_audit_actions.sql

-- TICKET_MESSAGE_POSTED et TICKET_PRIORITY_CHANGED ont été retirés de
-- l'enum AuditAction ; les lignes historiques pointent vers des valeurs
-- que l'enum ne contient plus (IllegalArgumentException au chargement).
-- On les remappe vers des valeurs existantes pour préserver l'historique.

UPDATE `audit_logs` SET `action` = 'TICKET_UPDATED'
WHERE `action` IN ('TICKET_MESSAGE_POSTED', 'TICKET_PRIORITY_CHANGED');
