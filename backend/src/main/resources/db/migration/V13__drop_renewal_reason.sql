-- =============================================================================
-- V13 : nettoyage — suppression de la colonne orpheline renewal_reason
-- =============================================================================
-- La fonctionnalité de renouvellement manuel a été retirée (V8/V9) ; la colonne
-- subscription_periods.renewal_reason n'est plus jamais écrite (toujours NULL).
-- On la supprime pour alléger le modèle.
-- =============================================================================

ALTER TABLE `subscription_periods` DROP COLUMN `renewal_reason`;