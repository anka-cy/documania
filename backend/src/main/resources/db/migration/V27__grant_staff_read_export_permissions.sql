-- Le portail staff promet l'accès aux pages opérationnelles (routeur) mais
-- V3 n'avait accordé à STAFF que TICKET_READ/TICKET_UPDATE (V16) : toutes les
-- autres pages répondaient 403. On accorde à STAFF la consultation et
-- l'export. Les créations/modifications/archivages, les suppressions, les
-- décisions de commande/abonnement, les comptes staff et les audits restent
-- réservés à ADMIN.
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT `staff_role`.`id`, `permission`.`id`
FROM `roles` AS `staff_role`
CROSS JOIN `permissions` AS `permission`
LEFT JOIN `role_permissions` AS `existing_assignment`
    ON `existing_assignment`.`role_id` = `staff_role`.`id`
   AND `existing_assignment`.`permission_id` = `permission`.`id`
WHERE `staff_role`.`name` = 'STAFF'
  AND `permission`.`code` IN (
      'DASHBOARD_READ',
      'CLIENT_READ',
      'SERVICE_READ',
      'OFFER_READ',
      'ORDER_READ',
      'SUBSCRIPTION_READ',
      'ARCHIVE_READ',
      'CLIENT_EXPORT',
      'ORDER_EXPORT',
      'SUBSCRIPTION_EXPORT'
  )
  AND `existing_assignment`.`role_id` IS NULL;
