-- Veviosys v2 - Restore original audit action values
-- Target: MySQL 8.4
-- Flyway migration: V24__restore_audit_action_values.sql

-- V23 avait remappé TICKET_MESSAGE_POSTED / TICKET_PRIORITY_CHANGED vers
-- TICKET_UPDATED, valeur qui n'existe pas non plus dans l'enum
-- (IllegalArgumentException au chargement de la page audits).
-- Correction : on restaure les valeurs d'origine. L'enum les redéclare
-- (lecture seule de l'historique) ; elles ne sont plus écrites par le code.
-- Restaure d'abord la priorité pour les anciennes lignes de changement
-- de priorité (new_values contient {"priority": ...}).
UPDATE `audit_logs` SET `action` = 'TICKET_PRIORITY_CHANGED'
WHERE `action` = 'TICKET_UPDATED' AND JSON_EXTRACT(`new_values`, '$.priority') IS NOT NULL;

-- Puis le reste (= messages postés).
UPDATE `audit_logs` SET `action` = 'TICKET_MESSAGE_POSTED'
WHERE `action` = 'TICKET_UPDATED' AND `entity_type` = 'TICKET';
