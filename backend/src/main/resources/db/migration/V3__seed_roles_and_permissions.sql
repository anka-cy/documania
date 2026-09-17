-- Veviosys v2 - Fixed roles and developer-defined permissions
-- Target: MySQL 8.4
-- Flyway migration: V3__seed_roles_and_permissions.sql

-- Keep the three fixed roles available without creating duplicates.
INSERT INTO `roles` (`name`, `description`)
SELECT 'ADMIN', 'Administrateur avec toutes les permissions'
WHERE NOT EXISTS (SELECT 1 FROM `roles` WHERE `name` = 'ADMIN');

INSERT INTO `roles` (`name`, `description`)
SELECT 'STAFF', 'Personnel avec des permissions configurables'
WHERE NOT EXISTS (SELECT 1 FROM `roles` WHERE `name` = 'STAFF');

INSERT INTO `roles` (`name`, `description`)
SELECT 'CLIENT', 'Client limité à son portail et à ses propres ressources'
WHERE NOT EXISTS (SELECT 1 FROM `roles` WHERE `name` = 'CLIENT');

-- Permission codes are created only by developers through Flyway migrations.
INSERT INTO `permissions` (`code`, `description`)
SELECT `seed`.`code`, `seed`.`description`
FROM (
    SELECT 'DASHBOARD_READ' AS `code`, 'Consulter le tableau de bord' AS `description`

    UNION ALL SELECT 'CLIENT_READ', 'Consulter les clients'
    UNION ALL SELECT 'CLIENT_CREATE', 'Créer des clients'
    UNION ALL SELECT 'CLIENT_UPDATE', 'Modifier des clients'
    UNION ALL SELECT 'CLIENT_ARCHIVE', 'Archiver des clients'
    UNION ALL SELECT 'CLIENT_RESTORE', 'Restaurer des clients'
    UNION ALL SELECT 'CLIENT_DELETE', 'Supprimer définitivement un client autorisé'

    UNION ALL SELECT 'SERVICE_READ', 'Consulter les services'
    UNION ALL SELECT 'SERVICE_CREATE', 'Créer des services'
    UNION ALL SELECT 'SERVICE_UPDATE', 'Modifier des services'
    UNION ALL SELECT 'SERVICE_ARCHIVE', 'Archiver des services'
    UNION ALL SELECT 'SERVICE_RESTORE', 'Restaurer des services'
    UNION ALL SELECT 'SERVICE_DELETE', 'Supprimer définitivement un service autorisé'

    UNION ALL SELECT 'OFFER_READ', 'Consulter les offres'
    UNION ALL SELECT 'OFFER_CREATE', 'Créer des offres'
    UNION ALL SELECT 'OFFER_UPDATE', 'Modifier des offres'
    UNION ALL SELECT 'OFFER_ARCHIVE', 'Archiver des offres'
    UNION ALL SELECT 'OFFER_RESTORE', 'Restaurer des offres'
    UNION ALL SELECT 'OFFER_DELETE', 'Supprimer définitivement une offre autorisée'

    UNION ALL SELECT 'ORDER_READ', 'Consulter les commandes'
    UNION ALL SELECT 'ORDER_CONFIRM', 'Confirmer des commandes'
    UNION ALL SELECT 'ORDER_REJECT', 'Rejeter des commandes'

    UNION ALL SELECT 'SUBSCRIPTION_READ', 'Consulter les abonnements'
    UNION ALL SELECT 'SUBSCRIPTION_CREATE', 'Créer des abonnements'
    UNION ALL SELECT 'SUBSCRIPTION_RENEW', 'Renouveler des abonnements'
    UNION ALL SELECT 'SUBSCRIPTION_CANCEL', 'Annuler des abonnements'

    UNION ALL SELECT 'STAFF_READ', 'Consulter les comptes staff'
    UNION ALL SELECT 'STAFF_CREATE', 'Créer des comptes staff'
    UNION ALL SELECT 'STAFF_UPDATE', 'Modifier des comptes staff'
    UNION ALL SELECT 'STAFF_ENABLE_DISABLE', 'Activer ou désactiver des comptes staff'
    UNION ALL SELECT 'STAFF_DELETE', 'Supprimer des comptes staff'

    UNION ALL SELECT 'PERMISSION_READ', 'Consulter les permissions'
    UNION ALL SELECT 'PERMISSION_ASSIGN', 'Attribuer des permissions au staff'
    UNION ALL SELECT 'PERMISSION_REMOVE', 'Retirer des permissions au staff'

    UNION ALL SELECT 'ARCHIVE_READ', 'Consulter les listes d archives'
    UNION ALL SELECT 'AUDIT_READ', 'Consulter les journaux d audit'

    UNION ALL SELECT 'CLIENT_EXPORT', 'Exporter les clients'
    UNION ALL SELECT 'ORDER_EXPORT', 'Exporter les commandes'
    UNION ALL SELECT 'SUBSCRIPTION_EXPORT', 'Exporter les abonnements'
) AS `seed`
LEFT JOIN `permissions` AS `existing_permission`
    ON `existing_permission`.`code` = `seed`.`code`
WHERE `existing_permission`.`id` IS NULL;

-- ADMIN receives every current permission. STAFF starts with no permissions.
-- CLIENT access is handled by authenticated ownership rules in the backend.
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT `admin_role`.`id`, `permission`.`id`
FROM `roles` AS `admin_role`
CROSS JOIN `permissions` AS `permission`
LEFT JOIN `role_permissions` AS `existing_assignment`
    ON `existing_assignment`.`role_id` = `admin_role`.`id`
   AND `existing_assignment`.`permission_id` = `permission`.`id`
WHERE `admin_role`.`name` = 'ADMIN'
  AND `existing_assignment`.`role_id` IS NULL;
