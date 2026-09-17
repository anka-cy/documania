-- L'envoi d'e-mails HTML n'a jamais été implémenté : tous les e-mails sont
-- en texte brut (la colonne html valait toujours false).
ALTER TABLE `email_outbox` DROP COLUMN `html`;
