-- =============================================================================
-- V11 : Suppression de client — garde les commandes/abonnements en historique
-- =============================================================================
-- Au lieu de RESTRICT (bloque la suppression), les FK passent en SET NULL :
-- supprimer un client archivé met client_id à NULL dans ses commandes et
-- abonnements, préservant les lignes pour l'historien métier via les colonnes
-- snapshot (client_company_name_snapshot, offer_name_snapshot, …).
-- =============================================================================

-- 1. Ajouter la colonne snapshot dans subscriptions (customer_orders l'a déjà)
ALTER TABLE `subscriptions`
    ADD COLUMN `client_company_name_snapshot` VARCHAR(150) NULL AFTER `cancelled_by`;

-- 2. Backfill : recopier le nom du company à partir du client actuel
UPDATE `subscriptions` `s`
    INNER JOIN `clients` `c` ON `s`.`client_id` = `c`.`id`
    SET `s`.`client_company_name_snapshot` = `c`.`company_name`;

-- 3. Rendre NOT NULL maintenant que toutes les lignes sont remplies
ALTER TABLE `subscriptions`
    MODIFY COLUMN `client_company_name_snapshot` VARCHAR(150) NOT NULL;

-- 4. Passer client_id à NULLABLE
ALTER TABLE `customer_orders`
    MODIFY COLUMN `client_id` BIGINT NULL;

ALTER TABLE `subscriptions`
    MODIFY COLUMN `client_id` BIGINT NULL;

-- 5. Supprimer les FK RESTRICT et les recréer en SET NULL.
--    (MySQL interdit de DROP + ré-ADD une FK du même nom dans un seul
--    ALTER TABLE : on sépare donc chaque opération.)
ALTER TABLE `customer_orders`
    DROP FOREIGN KEY `fk_customer_orders_client`;

ALTER TABLE `customer_orders`
    ADD CONSTRAINT `fk_customer_orders_client`
        FOREIGN KEY (`client_id`) REFERENCES `clients` (`id`) ON DELETE SET NULL;

ALTER TABLE `subscriptions`
    DROP FOREIGN KEY `fk_subscriptions_client`;

ALTER TABLE `subscriptions`
    ADD CONSTRAINT `fk_subscriptions_client`
        FOREIGN KEY (`client_id`) REFERENCES `clients` (`id`) ON DELETE SET NULL;