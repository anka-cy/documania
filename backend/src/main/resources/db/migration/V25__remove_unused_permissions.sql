-- Les 15 codes ci-dessous n'ont jamais été vérifiés par aucun endpoint
-- (les endpoints correspondants utilisent hasRole('ADMIN')). Ils sont
-- retirés de l'enum PermissionCode et de la base.
DELETE FROM `role_permissions`
WHERE `permission_id` IN (SELECT `id` FROM `permissions` WHERE `code` IN (
    'CLIENT_DELETE', 'SERVICE_DELETE', 'OFFER_DELETE',
    'ORDER_CONFIRM', 'ORDER_REJECT', 'SUBSCRIPTION_CANCEL',
    'STAFF_READ', 'STAFF_CREATE', 'STAFF_UPDATE', 'STAFF_ENABLE_DISABLE', 'STAFF_DELETE',
    'PERMISSION_READ', 'PERMISSION_ASSIGN', 'PERMISSION_REMOVE',
    'TICKET_DELETE'
));
DELETE FROM `permissions` WHERE `code` IN (
    'CLIENT_DELETE', 'SERVICE_DELETE', 'OFFER_DELETE',
    'ORDER_CONFIRM', 'ORDER_REJECT', 'SUBSCRIPTION_CANCEL',
    'STAFF_READ', 'STAFF_CREATE', 'STAFF_UPDATE', 'STAFF_ENABLE_DISABLE', 'STAFF_DELETE',
    'PERMISSION_READ', 'PERMISSION_ASSIGN', 'PERMISSION_REMOVE',
    'TICKET_DELETE'
);
