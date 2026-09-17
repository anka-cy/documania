-- =============================================================================
-- V12 : Suppression service/offre — cascade service→offres, historique sinon
-- =============================================================================
-- Aligne le modèle avec la règle métier :
--   - Supprimer un service supprime aussi ses offres (CASCADE).
--   - Supprimer une offre garde les commandes/abonnements en historique
--     (SET NULL + snapshots offer_name / service_name sur subscriptions).
-- =============================================================================

-- 1. Snapshots offer/service sur subscriptions (les commandes les ont déjà)
ALTER TABLE `subscriptions`
    ADD COLUMN `offer_name_snapshot` VARCHAR(100) NULL AFTER `client_company_name_snapshot`,
    ADD COLUMN `service_name_snapshot` VARCHAR(100) NULL AFTER `offer_name_snapshot`;

-- 2. Backfill depuis l'offre actuelle
UPDATE `subscriptions` `s`
    INNER JOIN `offers` `o` ON `s`.`offer_id` = `o`.`id`
    INNER JOIN `services` `sv` ON `o`.`service_id` = `sv`.`id`
    SET `s`.`offer_name_snapshot` = `o`.`name`,
        `s`.`service_name_snapshot` = `sv`.`name`;

-- 3. Rendre NOT NULL
ALTER TABLE `subscriptions`
    MODIFY COLUMN `offer_name_snapshot` VARCHAR(100) NOT NULL,
    MODIFY COLUMN `service_name_snapshot` VARCHAR(100) NOT NULL;

-- 4. Passer offer_id à NULLABLE (SET NULL à la suppression d'offre)
ALTER TABLE `customer_orders`
    MODIFY COLUMN `offer_id` BIGINT NULL;

ALTER TABLE `subscriptions`
    MODIFY COLUMN `offer_id` BIGINT NULL;

-- 5. FK : offres -> commandes/abonnements en SET NULL
--    (MySQL interdit DROP + ré-ADD d'une FK du même nom dans un seul ALTER)
ALTER TABLE `customer_orders`
    DROP FOREIGN KEY `fk_customer_orders_offer`;

ALTER TABLE `customer_orders`
    ADD CONSTRAINT `fk_customer_orders_offer`
        FOREIGN KEY (`offer_id`) REFERENCES `offers` (`id`) ON DELETE SET NULL;

ALTER TABLE `subscriptions`
    DROP FOREIGN KEY `fk_subscriptions_offer`;

ALTER TABLE `subscriptions`
    ADD CONSTRAINT `fk_subscriptions_offer`
        FOREIGN KEY (`offer_id`) REFERENCES `offers` (`id`) ON DELETE SET NULL;

-- 6. FK : offres -> service en CASCADE (supprimer un service supprime ses offres)
ALTER TABLE `offers`
    DROP FOREIGN KEY `fk_offers_service`;

ALTER TABLE `offers`
    ADD CONSTRAINT `fk_offers_service`
        FOREIGN KEY (`service_id`) REFERENCES `services` (`id`) ON DELETE CASCADE;